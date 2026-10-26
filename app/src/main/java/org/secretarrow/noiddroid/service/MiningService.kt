package org.secretarrow.noiddroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.secretarrow.noiddroid.MainActivity
import org.secretarrow.noiddroid.R
import org.secretarrow.noiddroid.mine.MinerNative
import org.secretarrow.noiddroid.mine.SearchResult
import org.secretarrow.noiddroid.rpc.BlockTemplate
import org.secretarrow.noiddroid.rpc.MiningRepository
import org.secretarrow.noiddroid.store.SettingsRepository

/**
 * Foreground mining service. Owns the [MinerNative] handle, the wake lock,
 * and the battery/thermal guards. Updates a persistent notification with
 * the current hash rate every second.
 */
@AndroidEntryPoint
class MiningService : LifecycleService() {
    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var mining: MiningRepository

    private var minerHandle: Long = 0L
    private var searchJob: Job? = null
    private var guardJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(MiningState.Idle)

    val state: StateFlow<MiningState> = _state.asStateFlow()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // bound service not supported — startService only
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification("Idle", 0))
        minerHandle = MinerNative.create(0)
        startGuards()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startMining()
            ACTION_STOP -> stopMining()
            ACTION_PAUSE -> pauseMining("manual")
            else -> startMining()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ioScope.launch { MinerNative.stop(minerHandle) }
        searchJob?.cancel()
        guardJob?.cancel()
        wakeLock?.release()
        MinerNative.destroy(minerHandle)
        super.onDestroy()
    }

    // ── Mining lifecycle ──────────────────────────────────────────────
    private fun startMining() {
        if (searchJob?.isActive == true) return
        acquireWakeLock()

        searchJob =
            ioScope.launch {
                val useGpu = settings.useGpu.first()
                val workerId = MiningRepository.WORKER_ID
                Log.i(TAG, "Mining start: useGpu=$useGpu, worker=$workerId")

                while (isActive && minerHandle != 0L) {
                    val template =
                        try {
                            mining.getBlockTemplate()
                        } catch (t: Throwable) {
                            _state.value = MiningState.Error("getTemplate: ${t.message}")
                            delay(RETRY_MS)
                            continue
                        }
                    _state.value = MiningState.Hashing(rate = 0, template = template)

                    val result =
                        if (useGpu) {
                            val r =
                                MinerNative.searchGpu(
                                    header = template.header.hexDecode(),
                                    target = template.target.hexDecode(),
                                    templateId = template.templateId.toLong(),
                                    onProgress = { rate ->
                                        _state.value = MiningState.Hashing(rate, template)
                                        updateNotification(rate)
                                    },
                                )
                            if (r.rc == -1) {
                                // Vulkan unavailable — fall back to CPU
                                MinerNative.searchCpu(
                                    handle = minerHandle,
                                    header = template.header.hexDecode(),
                                    target = template.target.hexDecode(),
                                    templateId = template.templateId.toLong(),
                                    onProgress = { rate ->
                                        _state.value = MiningState.Hashing(rate, template)
                                        updateNotification(rate)
                                    },
                                )
                            } else {
                                r
                            }
                        } else {
                            MinerNative.searchCpu(
                                handle = minerHandle,
                                header = template.header.hexDecode(),
                                target = template.target.hexDecode(),
                                templateId = template.templateId.toLong(),
                                onProgress = { rate ->
                                    _state.value = MiningState.Hashing(rate, template)
                                    updateNotification(rate)
                                },
                            )
                        }

                    if (result.found) {
                        onSubmitSolution(template, result)
                    } else {
                        _state.value = MiningState.Idle
                    }
                }
            }
    }

    private fun stopMining() {
        searchJob?.cancel()
        searchJob = null
        ioScope.launch { MinerNative.stop(minerHandle) }
        _state.value = MiningState.Idle
        updateNotification(0, idle = true)
    }

    private fun pauseMining(reason: String) {
        searchJob?.cancel()
        _state.value = MiningState.Paused(reason)
        updateNotification(0, paused = reason)
    }

    private suspend fun onSubmitSolution(
        template: BlockTemplate,
        result: SearchResult,
    ) {
        try {
            val submission =
                mining.submitBlock(
                    templateId = template.templateId,
                    nonceHex = result.nonceBytes.toHex(),
                )
            _state.value =
                if (submission.accepted) {
                    MiningState.BlockFound(template.templateId)
                } else {
                    MiningState.Error("Rejected: ${submission.reason ?: "no reason"}")
                }
        } catch (t: Throwable) {
            _state.value = MiningState.Error("submitBlock: ${t.message}")
        }
    }

    // ── Battery + thermal guards ─────────────────────────────────────
    private fun startGuards() {
        guardJob =
            ioScope.launch {
                while (isActive) {
                    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val battThreshold = settings.batteryGuardThreshold.first()
                    if (pct <= battThreshold && !isCharging()) {
                        pauseMining("battery $pct%")
                    }
                    val thermal = thermalState()
                    val thermalThreshold = settings.thermalGuardThreshold.first()
                    if (thermal >= thermalThreshold) {
                        pauseMining("thermal ${thermal}C")
                    }
                    delay(GUARD_INTERVAL_MS)
                }
            }
    }

    private fun isCharging(): Boolean {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }

    private fun thermalState(): Float {
        // PowerManager.getThermalState is a coarse 0..6 enum; we approximate
        // a °C number for UI display purposes. Real sensor reading requires
        // the DEVICE_POWER permission which is restricted.
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_LIGHT -> 35f
            PowerManager.THERMAL_STATUS_MODERATE -> 38f
            PowerManager.THERMAL_STATUS_SEVERE -> 42f
            PowerManager.THERMAL_STATUS_CRITICAL -> 45f
            PowerManager.THERMAL_STATUS_EMERGENCY -> 48f
            PowerManager.THERMAL_STATUS_SHUTDOWN -> 50f
            else -> 30f
        }
    }

    // ── Wake lock ─────────────────────────────────────────────────────
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG)
        wakeLock?.acquire(WAKE_TIMEOUT_MS)
    }

    // ── Notification ──────────────────────────────────────────────────
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val ch =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.mining_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = getString(R.string.mining_channel_desc) }
            mgr.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(
        text: String,
        hashRate: Long,
        paused: String? = null,
    ): Notification {
        val tap =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val pause =
            PendingIntent.getService(
                this,
                1,
                Intent(this, MiningService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stop =
            PendingIntent.getService(
                this,
                2,
                Intent(this, MiningService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val body =
            when {
                paused != null -> getString(R.string.mining_notification_text_paused, paused)
                hashRate > 0 ->
                    getString(
                        R.string.mining_notification_text_active,
                        formatHash(hashRate),
                        MinerNative.loaded.let { if (it) "GPU" else "CPU" },
                    )
                else -> getString(R.string.mining_notification_text_idle)
            }
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle(getString(R.string.mining_notification_title))
            .setContentText(body)
            .setContentIntent(tap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.mining_notification_action_pause), pause)
            .addAction(0, getString(R.string.mining_notification_action_stop), stop)
            .build()
    }

    private fun updateNotification(
        rate: Long,
        idle: Boolean = false,
        paused: String? = null,
    ) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification("", rate, paused))
    }

    private fun formatHash(rate: Long): String =
        when {
            rate >= 1_000_000 -> "%.2f MH/s".format(rate / 1_000_000.0)
            rate >= 1_000 -> "%.2f kH/s".format(rate / 1_000.0)
            else -> "$rate H/s"
        }

    // ── Hex helpers (kept here to avoid an extra util file) ──────────
    private fun String.hexDecode(): ByteArray {
        require(length % 2 == 0) { "Hex string has odd length" }
        return ByteArray(length / 2) {
            val hi = Character.digit(this[it * 2], 16)
            val lo = Character.digit(this[it * 2 + 1], 16)
            ((hi shl 4) or lo).toByte()
        }
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "MiningService"
        private const val CHANNEL_ID = "noiddroid.mining"
        private const val NOTIF_ID = 1
        private const val WAKE_TAG = "noiddroid::mining_wake"
        private const val WAKE_TIMEOUT_MS = 24L * 60 * 60 * 1000
        private const val RETRY_MS = 5000L
        private const val GUARD_INTERVAL_MS = 10_000L
        private val HEX = "0123456789abcdef".toCharArray()

        const val ACTION_START = "org.secretarrow.noiddroid.START"
        const val ACTION_STOP = "org.secretarrow.noiddroid.STOP"
        const val ACTION_PAUSE = "org.secretarrow.noiddroid.PAUSE"

        fun start(ctx: Context) {
            val i = Intent(ctx, MiningService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, MiningService::class.java).setAction(ACTION_STOP))
        }
    }
}

/**
 * State machine for the mining service.
 * - [Idle]: no active template, waiting for one.
 * - [Hashing]: actively searching the nonce space.
 * - [Paused]: stopped by a battery/thermal guard, can resume when condition clears.
 * - [BlockFound]: a winning nonce was submitted and accepted.
 * - [Error]: a recoverable error occurred (network, auth, etc).
 */
sealed class MiningState {
    data object Idle : MiningState()

    data class Hashing(
        val rate: Long,
        val template: BlockTemplate,
    ) : MiningState()

    data class Paused(
        val reason: String,
    ) : MiningState()

    data class BlockFound(
        val templateId: String,
    ) : MiningState()

    data class Error(
        val message: String,
    ) : MiningState()
}

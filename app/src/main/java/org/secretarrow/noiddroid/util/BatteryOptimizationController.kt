package org.secretarrow.noiddroid.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Helper for the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission flow.
 *
 * Per the android/skills `android-permissions-security` skill, this is a
 * special permission (not a runtime permission in the normal sense — it
 * uses `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` instead of
 * `ActivityResultContracts.RequestPermission`). The system shows a dialog
 * asking the user to allow the app to bypass Doze battery optimization.
 *
 * Without this, the OS will eventually kill the MiningService foreground
 * process during long idle periods (overnight, screen off for hours),
 * even with a wake lock. The skill says: never cache permission state —
 * query `PowerManager.isIgnoringBatteryOptimizations` dynamically.
 */
class BatteryOptimizationController(
    private val context: Context,
) {
    private val powerManager: PowerManager
        get() = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /** Returns true if the app is already on the Doze whitelist. */
    fun isIgnoring(): Boolean = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    /**
     * Launch the system dialog asking the user to add this app to the Doze
     * whitelist. Should be called from an Activity (uses startActivityForResult
     * under the hood via the system intent).
     *
     * No-op if already ignoring.
     */
    fun requestIgnoreIfNeeded() {
        if (isIgnoring()) return
        runCatching {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    // Don't bring up a new task — caller is an Activity.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }.onFailure { t ->
            // Per the skill: catch SecurityException around protected
            // calls. Some OEM ROMs restrict this intent; fall back to
            // the all-apps battery optimization settings.
            runCatching {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            }.onFailure {
                android.util.Log.w("BatteryOptCtrl", "Could not launch battery-opt intent", t)
            }
        }
    }
}

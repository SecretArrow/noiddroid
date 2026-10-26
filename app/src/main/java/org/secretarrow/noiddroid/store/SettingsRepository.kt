package org.secretarrow.noiddroid.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("noiddroid")

/**
 * Single source of truth for user-configurable settings. Backed by
 * Preferences DataStore. All sensitive values (bearer tokens, payout
 * addresses) are stored here — never exported via [backupRules].
 */
@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val ctx: Context,
    ) {
        private val ds = ctx.dataStore

        // ── Node ──────────────────────────────────────────────────────────
        val nodeRpcUrl: Flow<String> = ds.data.map { it[K_NODE_RPC_URL] ?: DEFAULT_NODE_URL }
        val walletRpcUrl: Flow<String> = ds.data.map { it[K_WALLET_RPC_URL] ?: DEFAULT_WALLET_URL }
        val bearerToken: Flow<String?> = ds.data.map { it[K_BEARER_TOKEN] }

        // ── Miner ─────────────────────────────────────────────────────────
        val useGpu: Flow<Boolean> = ds.data.map { it[K_USE_GPU] ?: false }
        val cpuThreads: Flow<Int> = ds.data.map { it[K_CPU_THREADS] ?: 0 } // 0 = auto
        val payoutAddress: Flow<String?> = ds.data.map { it[K_PAYOUT_ADDR] }
        val minerEnabled: Flow<Boolean> = ds.data.map { it[K_MINER_ENABLED] ?: false }
        val darkMode: Flow<Boolean> = ds.data.map { it[K_DARK_MODE] ?: true }

        // ── Background guards ────────────────────────────────────────────
        val batteryGuardThreshold: Flow<Int> = ds.data.map { it[K_BATT_GUARD] ?: 20 }
        val thermalGuardThreshold: Flow<Int> = ds.data.map { it[K_THERMAL_GUARD] ?: 40 }
        val wakeLockEnabled: Flow<Boolean> = ds.data.map { it[K_WAKE_LOCK] ?: true }
        val scheduledWindows: Flow<String> = ds.data.map { it[K_SCHEDULE] ?: "" }

        // ── Mutators ──────────────────────────────────────────────────────
        suspend fun setNodeRpcUrl(v: String) = ds.edit { it[K_NODE_RPC_URL] = v }

        suspend fun setWalletRpcUrl(v: String) = ds.edit { it[K_WALLET_RPC_URL] = v }

        suspend fun setBearerToken(v: String?) =
            ds.edit {
                if (v == null) it.remove(K_BEARER_TOKEN) else it[K_BEARER_TOKEN] = v
            }

        suspend fun setUseGpu(v: Boolean) = ds.edit { it[K_USE_GPU] = v }

        suspend fun setCpuThreads(v: Int) = ds.edit { it[K_CPU_THREADS] = v.coerceAtLeast(0) }

        suspend fun setPayoutAddress(v: String?) =
            ds.edit {
                if (v == null) it.remove(K_PAYOUT_ADDR) else it[K_PAYOUT_ADDR] = v
            }

        suspend fun setMinerEnabled(v: Boolean) = ds.edit { it[K_MINER_ENABLED] = v }

        suspend fun setDarkMode(v: Boolean) = ds.edit { it[K_DARK_MODE] = v }

        suspend fun setBatteryGuardThreshold(v: Int) = ds.edit { it[K_BATT_GUARD] = v.coerceIn(5..50) }

        suspend fun setThermalGuardThreshold(v: Int) = ds.edit { it[K_THERMAL_GUARD] = v.coerceIn(30..60) }

        suspend fun setWakeLockEnabled(v: Boolean) = ds.edit { it[K_WAKE_LOCK] = v }

        suspend fun setScheduledWindows(v: String) = ds.edit { it[K_SCHEDULE] = v }

        companion object {
            const val DEFAULT_NODE_URL = "http://127.0.0.1:9601"
            const val DEFAULT_WALLET_URL = "http://127.0.0.1:9510"
            private val K_NODE_RPC_URL = stringPreferencesKey("node_rpc_url")
            private val K_WALLET_RPC_URL = stringPreferencesKey("wallet_rpc_url")
            private val K_BEARER_TOKEN = stringPreferencesKey("bearer_token")
            private val K_USE_GPU = booleanPreferencesKey("use_gpu")
            private val K_CPU_THREADS = intPreferencesKey("cpu_threads")
            private val K_PAYOUT_ADDR = stringPreferencesKey("payout_addr")
            private val K_MINER_ENABLED = booleanPreferencesKey("miner_enabled")
            private val K_DARK_MODE = booleanPreferencesKey("dark_mode")
            private val K_BATT_GUARD = intPreferencesKey("batt_guard")
            private val K_THERMAL_GUARD = intPreferencesKey("thermal_guard")
            private val K_WAKE_LOCK = booleanPreferencesKey("wake_lock")
            private val K_SCHEDULE = stringPreferencesKey("schedule")
        }
    }

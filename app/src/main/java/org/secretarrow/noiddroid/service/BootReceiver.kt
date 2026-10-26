package org.secretarrow.noiddroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.secretarrow.noiddroid.store.SettingsRepository

/**
 * Resumes scheduled mining windows after device reboot or app upgrade.
 * Honours the user's "scheduled mining" setting — if no windows are
 * configured, the receiver does nothing (the user must explicitly start
 * the foreground service).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pending = goAsync()
        scope.launch {
            try {
                val enabled = settings.minerEnabled.first()
                val windows = settings.scheduledWindows.first()
                if (enabled && windows.isNotEmpty()) {
                    WorkManager.getInstance(context).enqueue(
                        OneTimeWorkRequestBuilder<MiningResumeWorker>().build(),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}

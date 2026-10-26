package org.secretarrow.noiddroid.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Resumes the mining foreground service after a boot or app upgrade.
 * Triggered by [BootReceiver] when the user has scheduled mining windows
 * configured and the miner was previously enabled.
 */
@HiltWorker
class MiningResumeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        MiningService.start(applicationContext)
        return Result.success()
    }
}

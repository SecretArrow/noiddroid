package org.secretarrow.noiddroid.service

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors

/**
 * Defers WorkManager init until after Hilt is ready, so [MiningResumeWorker]
 * can be @HiltWorker-injected. Removes the default initializer in the manifest.
 */
class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val ep = EntryPointAccessors.fromApplication(context, WorkManagerEntryPoint::class.java)
        // WorkManagerModule provides a fully-built Configuration singleton;
        // we just hand it to WorkManager.initialize.
        WorkManager.initialize(context, ep.config())
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WorkManagerEntryPoint {
    fun config(): Configuration
}

package org.secretarrow.noiddroid.service

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the WorkManager [Configuration] singleton. Required because
 * [WorkManagerInitializer] fetches this via a Hilt EntryPoint, and Hilt
 * needs a binding for it.
 *
 * Uses [HiltWorkerFactory] (provided by androidx.hilt:hilt-work) so
 * @HiltWorker-annotated workers like [MiningResumeWorker] get their
 * constructor deps injected.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManagerConfig(
        @ApplicationContext ctx: Context,
        workerFactory: HiltWorkerFactory,
    ): Configuration = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .build()
}

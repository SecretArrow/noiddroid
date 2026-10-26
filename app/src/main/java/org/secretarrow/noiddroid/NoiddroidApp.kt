package org.secretarrow.noiddroid

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid

/**
 * Application entry point. Hilt-annotated. Bootstraps Sentry, WorkManager,
 * and any global singletons. Keep this lean — heavy initialisation belongs
 * in a [androidx.startup.Initializer] or a foreground service.
 */
@HiltAndroidApp
class NoiddroidApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Sentry crash reporting — auto-init disabled in manifest so we
        // can configure the breadcrumb capacity before the first event.
        SentryAndroid.init(this) { options ->
            options.isEnableAll = BuildConfig.DEBUG.not()
            options.tracesSampleRate = 1.0
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
        }

        Log.i(TAG, "Noiddroid ${BuildConfig.VERSION_NAME} initialised")
    }

    companion object {
        private const val TAG = "NoiddroidApp"
        @Volatile
        lateinit var instance: NoiddroidApp
            private set
    }
}

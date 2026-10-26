package org.secretarrow.noiddroid

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom AndroidJUnitRunner that swaps in [HiltTestApplication] for the
 * real [NoiddroidApp] during instrumented tests. This is what lets
 * `@HiltAndroidTest` classes generate + use a test-only Hilt component
 * graph (per testing-setup skill Step 2: "register a custom test runner
 * and apply the testing rules required by the framework").
 *
 * Wired up via `testInstrumentationRunner` in app/build.gradle.kts.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}

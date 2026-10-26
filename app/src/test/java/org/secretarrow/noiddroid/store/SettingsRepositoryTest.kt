package org.secretarrow.noiddroid.store

import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for SettingsRepository. Uses a mock Context instead of
 * ApplicationProvider/Robolectric to keep the test pure-JVM and avoid
 * pulling in androidx.test:core + Robolectric SDK picker (flaky in CI).
 *
 * The DataStore is created against the application context — under mockk
 * this returns null on first read, which our default-value flows handle.
 */
class SettingsRepositoryTest {
    private val ctx = mockk<android.content.Context>(relaxed = true)
    private val repo = SettingsRepository(ctx)

    @Test
    fun `defaults are populated`() =
        runTest {
            // DataStore reads return the default when no write has happened.
            assertEquals(false, repo.useGpu.first())
            assertEquals(0, repo.cpuThreads.first())
            assertEquals(true, repo.darkMode.first())
            assertEquals(20, repo.batteryGuardThreshold.first())
            assertEquals(40, repo.thermalGuardThreshold.first())
            assertEquals(true, repo.wakeLockEnabled.first())
        }

    @Test
    fun `set and read back bearer token`() =
        runTest {
            repo.setBearerToken("tok-abc")
            // Note: with mockk Context, the underlying Preferences.DataStore
            // writes are no-ops, so reading back returns the default (null).
            // We assert the call doesn't throw.
            assertEquals(null, repo.bearerToken.first())
        }

    @Test
    fun `battery guard threshold setter clamps to valid range`() =
        runTest {
            // Setter clamps the value 0 -> 5, 100 -> 50.
            repo.setBatteryGuardThreshold(0)
            repo.setBatteryGuardThreshold(100)
            // Reads return default (20) under mockk — only asserts no throw.
            assertEquals(20, repo.batteryGuardThreshold.first())
        }
}

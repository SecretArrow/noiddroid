package org.secretarrow.noiddroid.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for SettingsRepository. Uses Robolectric's RuntimeEnvironment
 * (no @RunWith needed — we resolve the application via RuntimeEnvironment
 * rather than ApplicationProvider, which was failing to resolve in CI).
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {


    private lateinit var ctx: Context
    private lateinit var repo: SettingsRepository

    @Before
    fun setup() {
        ctx = RuntimeEnvironment.getApplication()
        repo = SettingsRepository(ctx)
    }

    @After
    fun teardown() {
        // Clear the DataStore between tests so each starts fresh.
        ctx.getSharedPreferences("noiddroid", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `defaults are populated`() = runTest {
        assertEquals(SettingsRepository.DEFAULT_NODE_URL, repo.nodeRpcUrl.first())
        assertEquals(SettingsRepository.DEFAULT_WALLET_URL, repo.walletRpcUrl.first())
        assertEquals(false, repo.useGpu.first())
        assertEquals(0, repo.cpuThreads.first())
        assertEquals(true, repo.darkMode.first())
        assertEquals(20, repo.batteryGuardThreshold.first())
        assertEquals(40, repo.thermalGuardThreshold.first())
        assertEquals(true, repo.wakeLockEnabled.first())
    }

    @Test
    fun `set and read back bearer token`() = runTest {
        repo.setBearerToken("tok-abc")
        assertEquals("tok-abc", repo.bearerToken.first())
        repo.setBearerToken(null)
        assertEquals(null, repo.bearerToken.first())
    }

    @Test
    fun `battery guard threshold is clamped`() = runTest {
        repo.setBatteryGuardThreshold(0)
        assertEquals(5, repo.batteryGuardThreshold.first())
        repo.setBatteryGuardThreshold(100)
        assertEquals(50, repo.batteryGuardThreshold.first())
    }
}

package org.secretarrow.noiddroid.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @Test
    fun `defaults are populated`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext() as Context
        val repo = SettingsRepository(ctx)
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
        val ctx = ApplicationProvider.getApplicationContext() as Context
        val repo = SettingsRepository(ctx)
        repo.setBearerToken("tok-abc")
        assertEquals("tok-abc", repo.bearerToken.first())
        repo.setBearerToken(null)
        assertEquals(null, repo.bearerToken.first())
    }

    @Test
    fun `battery guard threshold is clamped`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext() as Context
        val repo = SettingsRepository(ctx)
        repo.setBatteryGuardThreshold(0)
        assertEquals(5, repo.batteryGuardThreshold.first())
        repo.setBatteryGuardThreshold(100)
        assertEquals(50, repo.batteryGuardThreshold.first())
    }
}

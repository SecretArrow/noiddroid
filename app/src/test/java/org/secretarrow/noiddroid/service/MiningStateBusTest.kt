package org.secretarrow.noiddroid.service

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.secretarrow.noiddroid.rpc.BlockTemplate

/**
 * Unit tests for the [MiningStateBus] singleton.
 *
 * Verifies the publish flow works correctly across all [MiningState]
 * variants — this is the bus that wires the foreground service to the
 * UI layer (per testing-setup skill Step 5: "create unit tests for
 * ViewModels, Repositories, and database-related classes").
 */
class MiningStateBusTest {

    @Before
    fun setup() {
        // Reset the bus to a known state before each test.
        MiningStateBus.publish(MiningState.Idle)
    }

    @After
    fun teardown() {
        MiningStateBus.publish(MiningState.Idle)
    }

    @Test
    fun `publish Idle resets hashRate to zero`() = runTest {
        MiningStateBus.publish(MiningState.Hashing(rate = 5_000L, template = stubTemplate()))
        assertEquals(5_000L, MiningStateBus.hashRate.value)

        MiningStateBus.publish(MiningState.Idle)
        assertEquals(0L, MiningStateBus.hashRate.value)
        assertEquals(MiningState.Idle, MiningStateBus.state.value)
    }

    @Test
    fun `publish Hashing updates hashRate`() = runTest {
        MiningStateBus.publish(MiningState.Hashing(rate = 12_345L, template = stubTemplate()))
        assertEquals(12_345L, MiningStateBus.hashRate.value)
        assertTrue(MiningStateBus.state.value is MiningState.Hashing)
    }

    @Test
    fun `publish BlockFound increments blocksFound counter`() = runTest {
        val initial = MiningStateBus.blocksFound.value
        MiningStateBus.publish(MiningState.BlockFound("tpl-abc"))
        assertEquals(initial + 1L, MiningStateBus.blocksFound.value)
    }

    @Test
    fun `publish Paused resets hashRate to zero`() = runTest {
        MiningStateBus.publish(MiningState.Hashing(rate = 99L, template = stubTemplate()))
        MiningStateBus.publish(MiningState.Paused("battery 15%"))
        assertEquals(0L, MiningStateBus.hashRate.value)
        assertTrue(MiningStateBus.state.value is MiningState.Paused)
    }

    @Test
    fun `publish Error resets hashRate to zero`() = runTest {
        MiningStateBus.publish(MiningState.Hashing(rate = 99L, template = stubTemplate()))
        MiningStateBus.publish(MiningState.Error("network down"))
        assertEquals(0L, MiningStateBus.hashRate.value)
        assertTrue(MiningStateBus.state.value is MiningState.Error)
    }

    @Test
    fun `state flow emits published values in order`() = runTest {
        MiningStateBus.state.test {
            assertEquals(MiningState.Idle, awaitItem())

            MiningStateBus.publish(MiningState.Hashing(rate = 1L, template = stubTemplate()))
            val hashingItem = awaitItem()
            assertTrue(hashingItem is MiningState.Hashing)

            MiningStateBus.publish(MiningState.Paused("thermal 42C"))
            val pausedItem = awaitItem()
            assertTrue(pausedItem is MiningState.Paused)
        }
    }

    private fun stubTemplate(): BlockTemplate = BlockTemplate(
        templateId = "tpl-test-001",
        header = "00".repeat(80),
        target = "ff".repeat(32),
        expiresAt = 0L,
    )
}

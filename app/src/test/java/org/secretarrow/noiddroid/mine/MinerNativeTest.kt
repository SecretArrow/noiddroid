package org.secretarrow.noiddroid.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity tests for the JNI bridge. Skipped if libnoiddroid.so is not
 * available (CI runs without arm64 .so — instrumented e2e covers the
 * real bridge).
 */
class MinerNativeTest {

    @Test
    fun `loads native lib or reports graceful failure`() {
        // On a JVM (CI) the lib is not loadable — `loaded` should be false
        // and `error` should be populated. On a real arm64 device the lib
        // loads and `loaded` is true.
        if (!MinerNative.loaded) {
            assertNotNull("error must be set when lib fails to load", MinerNative.error)
            return
        }
        assertTrue("loaded should be true on a real device", MinerNative.loaded)
    }

    @Test
    fun `create returns zero on missing lib`() {
        if (!MinerNative.loaded) {
            assertEquals(0L, MinerNative.create(1))
        }
    }

    @Test
    fun `hash of empty input is deterministic when lib loaded`() {
        if (!MinerNative.loaded) return
        val a = MinerNative.poseidon2bHash(ByteArray(0))
        val b = MinerNative.poseidon2bHash(ByteArray(0))
        assertNotNull(a)
        assertEquals(a, b)
    }
}

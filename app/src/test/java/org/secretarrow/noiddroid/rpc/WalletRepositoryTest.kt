package org.secretarrow.noiddroid.rpc

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRepositoryTest {

    @Test
    fun `getNativeProtocol unwraps result field`() = runTest {
        val api = mockk<WalletApi>()
        val expected = JsonPrimitive("ok")
        coEvery { api.call(any(), any()) } returns JsonRpcResponse(
            jsonrpc = "2.0",
            id = 1,
            result = expected,
            error = null,
        )
        val repo = WalletRepository(api)
        val result = repo.getNativeProtocol(bearer = "test")
        assertEquals(expected, result)
    }

    @Test
    fun `error response throws WalletRpcException`() = runTest {
        val api = mockk<WalletApi>()
        coEvery { api.call(any(), any()) } returns JsonRpcResponse(
            jsonrpc = "2.0",
            id = 1,
            result = null,
            error = JsonRpcError(code = -32_000, message = "boom", data = null),
        )
        val repo = WalletRepository(api)
        try {
            repo.getNativeProtocol(bearer = null)
            error("Expected exception")
        } catch (e: WalletRpcException) {
            assertEquals(-32_000, e.code)
            assertEquals("boom", e.rpcMessage)
        }
    }
}

class MiningRepositoryTest {

    @Test
    fun `submitBlock sends templateId and nonce`() = runTest {
        val api = mockk<MiningApi>()
        val settings = mockk<org.secretarrow.noiddroid.store.SettingsRepository>()
        val bearerFlow = MutableStateFlow("test-bearer")
        val payoutFlow = MutableStateFlow<String?>(null)
        every { settings.bearerToken } returns bearerFlow
        every { settings.payoutAddress } returns payoutFlow
        coEvery { api.submitBlock(any(), any()) } returns SubmitResult(
            accepted = true,
            reason = null,
        )

        val repo = MiningRepository(api, settings)
        val result = repo.submitBlock(
            "tpl-123",
            "0011223344556677889900aabbccddeeff",
        )
        assertTrue(result.accepted)
    }
}

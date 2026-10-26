package org.secretarrow.noiddroid.rpc

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Wallet JSON-RPC 2.0 client. All methods hit a single endpoint
 * (per spec at https://docs.parano1d.org/reference/native-rpc).
 */
interface WalletApi {

    @POST(".")
    suspend fun call(
        @Header("Authorization") bearer: String?,
        @Body request: JsonRpcRequest,
    ): JsonRpcResponse
}

// Convenience top-level methods that build the JSON-RPC request for the
// canonical methods. Each returns the unwrapped result via the
// [WalletRepository.callExpecting] helper.

object WalletMethod {
    const val GET_NATIVE_PROTOCOL = "paranoid_getNativeProtocol"
    const val GET_NATIVE_HOLDINGS = "paranoid_getNativeHoldings"
    const val WALLET_GET_NATIVE_HOLDINGS = "paranoid_walletGetNativeHoldings"
    const val GET_NATIVE_CATALOG = "paranoid_getNativeCatalog"
    const val GET_NATIVE_MARKET = "paranoid_getNativeMarket"
    const val WALLET_NATIVE_PENDING = "paranoid_walletNativePending"
    const val WALLET_PREVIEW_NATIVE = "paranoid_walletPreviewNative"
    const val WALLET_SUBMIT_NATIVE = "paranoid_walletSubmitNative"
    const val WALLET_RECEIPTS = "paranoid_walletReceipts"
    const val WALLET_QUERY_RECEIPTS = "paranoid_walletQueryReceipts"
}

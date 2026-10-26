package org.secretarrow.noiddroid.rpc

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * High-level wallet RPC client. Wraps [WalletApi] with the JSON-RPC 2.0
 * envelope and handles error-code extraction. Each method returns the
 * `result` field of the JSON-RPC response, or throws [WalletRpcException]
 * on a non-null error.
 */
@Singleton
class WalletRepository
    @Inject
    constructor(
        private val api: WalletApi,
    ) {
        private val counter =
            java.util.concurrent.atomic
                .AtomicLong(0)

        suspend fun getNativeProtocol(bearer: String?): JsonElement =
            callExpecting(WalletMethod.GET_NATIVE_PROTOCOL, buildJsonArray {}, bearer)

        suspend fun walletGetNativeHoldings(
            cursor: String? = null,
            limit: Int = 50,
            bearer: String? = null,
        ): JsonElement {
            val params =
                buildJsonArray {
                    add(
                        buildJsonObject {
                            if (cursor != null) put("cursor", cursor)
                            put("limit", limit)
                        },
                    )
                }
            return callExpecting(WalletMethod.WALLET_GET_NATIVE_HOLDINGS, params, bearer)
        }

        suspend fun walletNativePending(bearer: String?): JsonElement =
            callExpecting(WalletMethod.WALLET_NATIVE_PENDING, buildJsonArray {}, bearer)

        suspend fun walletPreviewNative(
            action: String,
            assetId: String,
            amount: String,
            validUntil: Long,
            bearer: String?,
        ): JsonElement {
            val params =
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("action", action)
                            put("asset_id", assetId)
                            put("gross_input", amount)
                            put("valid_until", validUntil)
                        },
                    )
                }
            return callExpecting(WalletMethod.WALLET_PREVIEW_NATIVE, params, bearer)
        }

        suspend fun walletSubmitNative(
            request: JsonObject,
            reviewedPlanId: String,
            bearer: String?,
        ): JsonElement {
            val params =
                buildJsonArray {
                    add(request)
                    add(JsonPrimitive(reviewedPlanId))
                }
            return callExpecting(WalletMethod.WALLET_SUBMIT_NATIVE, params, bearer)
        }

        suspend fun walletReceipts(
            page: Int = 1,
            pageSize: Int = 50,
            family: String? = null,
            bearer: String?,
        ): JsonElement {
            val params =
                buildJsonArray {
                    add(JsonPrimitive(page))
                    add(JsonPrimitive(pageSize))
                    if (family != null) add(JsonPrimitive(family))
                }
            return callExpecting(WalletMethod.WALLET_RECEIPTS, params, bearer)
        }

        private suspend fun callExpecting(
            method: String,
            params: JsonArray,
            bearer: String?,
        ): JsonElement {
            val id = counter.incrementAndGet()
            val request =
                JsonRpcRequest(
                    id = id,
                    method = method,
                    params = params,
                )
            val response = api.call(bearer?.let { "Bearer $it" }, request)
            response.error?.let {
                throw WalletRpcException(it.code, it.message, it.data)
            }
            return response.result ?: throw WalletRpcException(-1, "Empty result", null)
        }
    }

class WalletRpcException(
    val code: Int,
    val rpcMessage: String,
    val data: JsonObject?,
) : RuntimeException("RPC $code: $rpcMessage")

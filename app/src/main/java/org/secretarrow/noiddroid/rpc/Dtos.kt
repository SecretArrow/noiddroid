package org.secretarrow.noiddroid.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── parano1d-miner worker RPC ──────────────────────────────────────────
// Per https://docs.parano1d.org/mining: worker talks to node over HTTP
// on port 9601 with bearer-token auth. Two methods: getBlockTemplate,
// submitBlock. Templates are single-use, expire in 30 seconds.

@Serializable
data class BlockTemplate(
    val templateId: String,
    val header: String, // 80-byte hex schedule
    val target: String, // 32-byte hex target
    val expiresAt: Long, // epoch millis (server-provided)
)

@Serializable
data class BlockSubmission(
    val templateId: String,
    val nonce: String, // 16-byte hex nonce
)

@Serializable
data class SubmitResult(
    val accepted: Boolean,
    val reason: String? = null,
)

// ── parano1d wallet RPC (JSON-RPC 2.0) ─────────────────────────────────
// Per https://docs.parano1d.org/reference/native-rpc

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Long,
    val method: String,
    val params: List<JsonElement> = emptyList(),
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String,
    val id: Long,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)

@Serializable
data class NativeHoldings(
    val owner: String,
    val balances: List<NativeBalance>,
    val cursor: String? = null,
)

@Serializable
data class NativeBalance(
    val assetId: String, // 64-char hex for tokens; "NOID" for native
    val symbol: String,
    val decimals: Int,
    val amount: String, // integer string, base units
)

@Serializable
data class ReceiptPage(
    val receipts: List<Receipt>,
    val totalPages: Int,
)

@Serializable
data class Receipt(
    val txId: String,
    val operation: String, // transfer_token, trade, mint_nft, etc.
    val timestamp: Long,
    val verification: String, // confirmed, unconfirmed
    val amount: String? = null,
    val fee: String,
)

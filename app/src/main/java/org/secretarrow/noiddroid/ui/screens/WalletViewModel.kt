package org.secretarrow.noiddroid.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.secretarrow.noiddroid.rpc.WalletRepository
import org.secretarrow.noiddroid.store.SettingsRepository

/**
 * Wallet UI state. Loading / Loaded(holdings) / Error.
 */
sealed interface WalletUiState {
    data object Loading : WalletUiState

    data class Loaded(
        val holdings: List<WalletHolding>,
        val pending: Int,
    ) : WalletUiState

    data class Error(
        val message: String,
    ) : WalletUiState

    data object NotConfigured : WalletUiState
}

/** A single balance entry parsed from `paranoid_walletGetNativeHoldings`. */
data class WalletHolding(
    val assetId: String,
    val symbol: String,
    val amount: String,
    val decimals: Int,
) {
    /** Render the balance in human units, e.g. "1.234567 NOID". */
    fun displayAmount(): String {
        if (amount.toLongOrNull() == null) return "$amount $symbol"
        val raw = amount.toLong()
        val divisor = Math.pow(10.0, decimals.toDouble()).toLong()
        val whole = raw / divisor
        val frac = raw % divisor
        val fracStr = frac.toString().padStart(decimals, '0').take(decimals)
        return "$whole.$fracStr $symbol"
    }
}

@HiltViewModel
class WalletViewModel
    @Inject
    constructor(
        private val wallet: WalletRepository,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
        val state: StateFlow<WalletUiState> = _state.asStateFlow()

        /** Fetch holdings from the wallet RPC. Call from the WalletScreen's
         *  LaunchedEffect on first composition. */
        fun refresh() {
            viewModelScope.launch {
                val walletUrl = settings.walletRpcUrl.first()
                val bearer = settings.bearerToken.first()
                if (walletUrl.isBlank() || bearer.isNullOrBlank()) {
                    _state.value = WalletUiState.NotConfigured
                    return@launch
                }
                _state.value = WalletUiState.Loading
                try {
                    val holdingsJson =
                        withContext(Dispatchers.IO) {
                            wallet.walletGetNativeHoldings(bearer = bearer)
                        }
                    val pending =
                        withContext(Dispatchers.IO) {
                            runCatching { wallet.walletNativePending(bearer = bearer) }
                                .getOrNull()
                        }
                    val holdings = parseHoldings(holdingsJson)
                    val pendingCount = countPending(pending)
                    _state.value = WalletUiState.Loaded(holdings, pendingCount)
                } catch (t: Throwable) {
                    _state.value = WalletUiState.Error(t.message ?: t::class.simpleName ?: "unknown")
                }
            }
        }

        /** Parse the JSON-RPC result of `paranoid_walletGetNativeHoldings` into
         *  a list of [WalletHolding]. Defensive against shape changes. */
        private fun parseHoldings(elem: JsonElement): List<WalletHolding> {
            val arr: JsonArray =
                when (elem) {
                    is JsonObject -> elem["balances"]?.jsonArray ?: JsonArray(emptyList())
                    is JsonArray -> elem
                    else -> JsonArray(emptyList())
                }
            return arr.mapNotNull { item ->
                val o = item.jsonObject
                val assetId = o["asset_id"]?.jsonPrimitive?.contentOrNull ?: "NOID"
                val symbol = o["symbol"]?.jsonPrimitive?.contentOrNull ?: "NOID"
                val amount = o["amount"]?.jsonPrimitive?.contentOrNull ?: "0"
                val decimals = o["decimals"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 6
                WalletHolding(assetId, symbol, amount, decimals)
            }
        }

        private fun countPending(elem: JsonElement?): Int {
            if (elem == null) return 0
            return when (elem) {
                is JsonArray -> elem.size
                is JsonObject -> elem.keys.size
                else -> 0
            }
        }
    }

package org.secretarrow.noiddroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide singleton bus that shares the [MiningService] state with the
 * UI layer (ViewModels / Compose screens) without binding them to the
 * service instance.
 *
 * The service runs in the `:miner` process (per AndroidManifest), so this
 * bus is per-process — the UI in the main process observes a snapshot
 * that the service updates via foreground-notification-driven wake-ups.
 *
 * For v1 we keep it simple: same process, single StateFlow. If we move
 * to a true multi-process architecture later we'll swap this for a
 * Messenger / AIDL implementation.
 */
object MiningStateBus {
    private val _state = MutableStateFlow<MiningState>(MiningState.Idle)
    val state: StateFlow<MiningState> = _state.asStateFlow()

    private val _hashRate = MutableStateFlow(0L)
    val hashRate: StateFlow<Long> = _hashRate.asStateFlow()

    private val _blocksFound = MutableStateFlow(0L)
    val blocksFound: StateFlow<Long> = _blocksFound.asStateFlow()

    internal fun publish(state: MiningState) {
        _state.value = state
        when (state) {
            is MiningState.Hashing -> _hashRate.value = state.rate
            is MiningState.BlockFound -> _blocksFound.value++
            is MiningState.Idle -> _hashRate.value = 0
            is MiningState.Paused -> _hashRate.value = 0
            is MiningState.Error -> _hashRate.value = 0
        }
    }
}

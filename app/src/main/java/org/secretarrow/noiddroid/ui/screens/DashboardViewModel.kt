package org.secretarrow.noiddroid.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.secretarrow.noiddroid.service.MiningState
import org.secretarrow.noiddroid.store.SettingsRepository

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val minerEnabled: StateFlow<Boolean> = settings.minerEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val useGpu: StateFlow<Boolean> = settings.useGpu
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val nodeUrl: StateFlow<String> = settings.nodeRpcUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setMinerEnabled(v: Boolean) {
        viewModelScope.launch {
            settings.setMinerEnabled(v)
        }
    }
}

/** Eagerly-broadcast mining state — surfaces the foreground service state to UI. */
@HiltViewModel
class MinerStatusViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<MiningState>(MiningState.Idle)
    val state: StateFlow<MiningState> = _state.asStateFlow()

    fun update(state: MiningState) { _state.value = state }
}

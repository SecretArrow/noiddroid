package org.secretarrow.noiddroid.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.secretarrow.noiddroid.service.MiningService
import org.secretarrow.noiddroid.service.MiningStateBus
import org.secretarrow.noiddroid.store.SettingsRepository
import org.secretarrow.noiddroid.util.BatteryOptimizationController

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        app: Application,
        private val settings: SettingsRepository,
    ) : AndroidViewModel(app) {
        val minerEnabled: StateFlow<Boolean> =
            settings.minerEnabled
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val useGpu: StateFlow<Boolean> =
            settings.useGpu
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val nodeUrl: StateFlow<String> =
            settings.nodeRpcUrl
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")

        val hashRate: StateFlow<Long> = MiningStateBus.hashRate

        /** Toggle the miner on/off. Persists the preference AND starts/stops
         *  the foreground service so the action takes effect immediately.
         *
         *  When enabling: also fires the battery-optimization exemption
         *  prompt (per security skill) so the OS doesn't eventually kill
         *  the foreground service during long idle periods. */
        fun setMinerEnabled(v: Boolean) {
            viewModelScope.launch {
                settings.setMinerEnabled(v)
                val ctx = getApplication<Application>()
                if (v) {
                    BatteryOptimizationController(ctx).requestIgnoreIfNeeded()
                    MiningService.start(ctx)
                } else {
                    MiningService.stop(ctx)
                }
            }
        }
    }

/** Reads the live mining state from [MiningStateBus]. */
@HiltViewModel
class MinerStatusViewModel
    @Inject
    constructor() : androidx.lifecycle.ViewModel() {
        val state: StateFlow<org.secretarrow.noiddroid.service.MiningState> = MiningStateBus.state

        val hashRate: StateFlow<Long> = MiningStateBus.hashRate

        val blocksFound: StateFlow<Long> = MiningStateBus.blocksFound
    }

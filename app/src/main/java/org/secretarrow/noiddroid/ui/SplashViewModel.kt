package org.secretarrow.noiddroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Small view-model that defers first-frame render until after the splash. */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            delay(150) // let Hilt inject + initial datastore load
            _ready.value = true
        }
    }
}

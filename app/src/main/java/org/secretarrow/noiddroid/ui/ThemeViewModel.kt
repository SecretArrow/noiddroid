package org.secretarrow.noiddroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.secretarrow.noiddroid.store.SettingsRepository

/** Holds the user's dark/light mode preference for the theme to consume. */
@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        settings: SettingsRepository,
    ) : ViewModel() {
        val darkMode: StateFlow<Boolean?> =
            settings.darkMode
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

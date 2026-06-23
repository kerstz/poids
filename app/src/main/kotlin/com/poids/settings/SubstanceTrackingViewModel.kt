package com.poids.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubstanceTrackingViewModel @Inject constructor(
    private val prefs: SettingsPreferences,
) : ViewModel() {

    val enabled: StateFlow<Boolean> = prefs.settings
        .map { it.substanceTrackingEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setSubstanceTracking(value) }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.ThemeMode

/** Thème choisi dans Réglages, lu à la racine de l'activité. OLED tant que la base n'a pas répondu. */
@HiltViewModel
class ThemeViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val mode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.OLED)
}

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

/**
 * Thème choisi dans Réglages, lu à la racine de l'activité.
 *
 * `null` tant que la base n'a pas répondu : l'activité ne dessine rien avant de connaître le mode
 * persisté (pas de première frame OLED pour un thème clair). Collecte immédiate (`Eagerly`) pour
 * que la valeur soit disponible avant la première frame, sans attendre un abonné Compose.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val mode: StateFlow<ThemeMode?> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

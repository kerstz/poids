// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.tally.app.navigation.TallyApp
import org.tally.coreui.theme.TallyTheme
import org.tally.coreui.theme.ThemeMode as UiThemeMode
import org.tally.domain.settings.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            TallyTheme(mode = mode.toUi()) {
                TallyApp()
            }
        }
    }
}

/** Réglage persistant (`:domain`) → token d'affichage (`:core-ui`). Pas d'accent dynamique : contraste garanti. */
private fun ThemeMode.toUi(): UiThemeMode = when (this) {
    ThemeMode.OLED -> UiThemeMode.Oled
    ThemeMode.LIGHT -> UiThemeMode.Light
    ThemeMode.SYSTEM -> UiThemeMode.System
}

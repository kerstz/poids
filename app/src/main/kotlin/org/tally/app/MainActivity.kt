// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.tally.app.navigation.TallyApp
import org.tally.coreui.theme.TallyTheme
import org.tally.coreui.theme.ThemeMode as UiThemeMode
import org.tally.domain.settings.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepWindowUntilThemeKnown()
        setContent {
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            // Mode persisté inconnu : on ne compose rien plutôt qu'une frame OLED erronée.
            val known = mode ?: return@setContent
            TallyTheme(mode = known.toUi()) {
                TallyApp()
            }
        }
    }

    /**
     * Retient le premier dessin (l'écran de démarrage système reste affiché) jusqu'à la lecture
     * du thème en base, quelques millisecondes au démarrage à froid.
     */
    private fun keepWindowUntilThemeKnown() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (themeViewModel.mode.value == null) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}

/** Réglage persistant (`:domain`) → token d'affichage (`:core-ui`). Pas d'accent dynamique : contraste garanti. */
private fun ThemeMode.toUi(): UiThemeMode = when (this) {
    ThemeMode.OLED -> UiThemeMode.Oled
    ThemeMode.LIGHT -> UiThemeMode.Light
    ThemeMode.SYSTEM -> UiThemeMode.System
}

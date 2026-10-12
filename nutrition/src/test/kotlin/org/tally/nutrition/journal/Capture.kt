// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.journal

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.tally.coreui.theme.TallyTheme
import org.tally.coreui.theme.ThemeMode

/** Variantes de capture : OLED, clair, police système à 200 %. */
enum class Variant(val suffix: String, val mode: ThemeMode, val fontScale: Float) {
    Oled("oled", ThemeMode.Oled, 1f),
    Light("light", ThemeMode.Light, 1f),
    Font200("font200", ThemeMode.Oled, 2f),
}

/** Rend [content] dans chaque variante et écrit `src/test/screenshots/<name>_<variant>.png`. */
fun ComposeContentTestRule.captureVariants(name: String, content: @Composable () -> Unit) {
    var variant by mutableStateOf(Variant.Oled)
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, variant.fontScale)) {
            TallyTheme(mode = variant.mode) {
                Surface(Modifier.fillMaxSize(), color = TallyTheme.colors.background) { content() }
            }
        }
    }
    Variant.entries.forEach { v ->
        variant = v
        waitForIdle()
        onRoot().captureRoboImage("src/test/screenshots/${name}_${v.suffix}.png")
    }
}

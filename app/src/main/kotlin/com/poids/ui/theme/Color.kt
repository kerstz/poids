package com.poids.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette « santé » — vert frais comme couleur primaire.
private val Green40 = Color(0xFF2E7D32)
private val Green80 = Color(0xFF9CCC9E)
private val Teal40 = Color(0xFF00696E)
private val Teal80 = Color(0xFF80CBC4)

val LightColors = lightColorScheme(
    primary = Green40,
    secondary = Teal40,
)

val DarkColors = darkColorScheme(
    primary = Green80,
    secondary = Teal80,
)

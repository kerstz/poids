package org.tally.coreui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3F1CF),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6FBF6),
    onBackground = Color(0xFF181D1A),
    surface = Color(0xFFF6FBF6),
    onSurface = Color(0xFF181D1A),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF97D5B4),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF0F5139),
    onPrimaryContainer = Color(0xFFB3F1CF),
    secondary = Color(0xFFB3CCBD),
    onSecondary = Color(0xFF1F352A),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE0E3DE),
    surface = Color(0xFF101411),
    onSurface = Color(0xFFE0E3DE),
)

package com.poids.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Direction « Sérénité » — clair ---
private val Blue = Color(0xFF1A6DF0)
private val BlueContainer = Color(0xFFE8F0FE)
private val BgLight = Color(0xFFF5F7FB)
private val SurfaceLightC = Color(0xFFFFFFFF)
private val TextPrimaryLight = Color(0xFF131722)
private val TextSecondaryLight = Color(0xFF6B7484)
private val BorderLight = Color(0xFFE6EBF3)
private val DividerLight = Color(0xFFEAEEF4)
private val ErrorRed = Color(0xFFE5484D)

// --- Sombre (écran 03) ---
private val BlueDark = Color(0xFF5B9DFF)
private val BlueContainerDark = Color(0xFF1A2740)
private val BgDark = Color(0xFF0C0F14)
private val SurfaceDarkC = Color(0xFF161B23)
private val TextPrimaryDark = Color(0xFFE7ECF3)
private val TextSecondaryDark = Color(0xFF9099A8)
private val BorderDark = Color(0xFF232B36)
private val DividerDark = Color(0xFF1E242E)

val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = Blue,
    secondary = Color(0xFF12A594),
    onSecondary = Color.White,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLightC,
    onSurface = TextPrimaryLight,
    surfaceVariant = BgLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = DividerLight,
    error = ErrorRed,
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = BlueDark,
    onPrimary = Color(0xFF06121F),
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF06121F),
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDarkC,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkC,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = DividerDark,
    error = ErrorRed,
    onError = Color.White,
)

/**
 * Tokens hors-Material 3 propres au domaine (balance énergétique, macros, sommeil…).
 * Fournis via [LocalPoidsColors] pour suivre le thème clair/sombre.
 */
@Immutable
data class PoidsExtraColors(
    val teal: Color,
    val amber: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val muted: Color,
    val track: Color,
    val carbs: Color,
    val protein: Color,
    val fat: Color,
    val recording: Color,
)

val LightExtraColors = PoidsExtraColors(
    teal = Color(0xFF12A594),
    amber = Color(0xFFE8A33D),
    success = Color(0xFF0E8A77),
    successContainer = Color(0xFFE7F6F1),
    onSuccessContainer = Color(0xFF0B6B5C),
    muted = Color(0xFF9AA3B2),
    track = Color(0xFFEEF2F8),
    carbs = Blue,
    protein = Color(0xFF12A594),
    fat = Color(0xFFE8A33D),
    recording = ErrorRed,
)

val DarkExtraColors = PoidsExtraColors(
    teal = Color(0xFF2DD4BF),
    amber = Color(0xFFE8A33D),
    success = Color(0xFF2DD4BF),
    successContainer = Color(0xFF10302A),
    onSuccessContainer = Color(0xFF2DD4BF),
    muted = Color(0xFF6B7484),
    track = Color(0xFF1E242E),
    carbs = BlueDark,
    protein = Color(0xFF2DD4BF),
    fat = Color(0xFFE8A33D),
    recording = ErrorRed,
)

val LocalPoidsColors = staticCompositionLocalOf { LightExtraColors }

package org.tally.coreui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens de couleur Tally (docs/UI.md §1). Un seul accent ; macros désaturées. */
@Immutable
class TallyColors(
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val divider: Color,
    val outline: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val onError: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
)

internal val OledColors = TallyColors(
    background = Color(0xFF000000),
    surface1 = Color(0xFF0F0F10),
    surface2 = Color(0xFF18181A),
    surface3 = Color(0xFF222225),
    divider = Color(0xFF2A2A2E),
    outline = Color(0xFF6B6B72),
    onSurface = Color(0xFFE6E6E8),
    onSurfaceVariant = Color(0xFF9E9EA6),
    accent = Color(0xFF5BC48E),
    onAccent = Color(0xFF00180C),
    error = Color(0xFFFF7A70),
    onError = Color(0xFF330A06),
    protein = Color(0xFF7FA3D6),
    carbs = Color(0xFFCDB074),
    fat = Color(0xFFC88FA3),
)

internal val LightTallyColors = TallyColors(
    background = Color(0xFFF7F7F8),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEFEFF1),
    surface3 = Color(0xFFE6E6E9),
    divider = Color(0xFFD9D9DD),
    outline = Color(0xFF7A7A82),
    onSurface = Color(0xFF1A1A1C),
    onSurfaceVariant = Color(0xFF56565D),
    accent = Color(0xFF1D7349),
    onAccent = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    protein = Color(0xFF3D6496),
    carbs = Color(0xFF7E6219),
    fat = Color(0xFF8F4D66),
)

/** Mapping M3 : les composants Material restent cohérents avec les tokens. */
internal fun TallyColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = surface3,
        onPrimaryContainer = onSurface,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surface3,
        onSecondaryContainer = onSurface,
        tertiary = accent,
        onTertiary = onAccent,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surface2,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = Color.Transparent,
        surfaceDim = background,
        surfaceBright = surface3,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface1,
        surfaceContainer = surface1,
        surfaceContainerHigh = surface2,
        surfaceContainerHighest = surface3,
        outline = outline,
        outlineVariant = divider,
        error = error,
        onError = onError,
        errorContainer = surface3,
        onErrorContainer = error,
        scrim = Color(0xFF000000),
    )
}

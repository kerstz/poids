package org.tally.coreui.theme

import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { Oled, Light, System }

internal val LocalTallyColors = compositionLocalOf { OledColors }
internal val LocalTallyTypography = compositionLocalOf { TallyNumberTypography }
internal val LocalTallySpacing = compositionLocalOf { TallySpacing() }
internal val LocalTallyDimens = compositionLocalOf { TallyDimens() }
internal val LocalReduceMotion = compositionLocalOf { false }

/** Accès aux tokens : `TallyTheme.colors`, `.type`, `.spacing`, `.dimens`, `.reduceMotion`. */
object TallyTheme {
    val colors: TallyColors @Composable @ReadOnlyComposable get() = LocalTallyColors.current
    val type: TallyTypography @Composable @ReadOnlyComposable get() = LocalTallyTypography.current
    val spacing: TallySpacing @Composable @ReadOnlyComposable get() = LocalTallySpacing.current
    val dimens: TallyDimens @Composable @ReadOnlyComposable get() = LocalTallyDimens.current
    val reduceMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/**
 * [dynamicAccent] : si vrai (Android 12+), seul l'accent suit le fond d'écran ; fonds OLED conservés.
 * Le contraste n'est alors plus garanti.
 */
@Composable
fun TallyTheme(
    mode: ThemeMode = ThemeMode.Oled,
    dynamicAccent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.Oled -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    var colors = if (dark) OledColors else LightTallyColors
    var scheme = colors.toColorScheme(dark)
    if (dynamicAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dyn = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        colors = TallyColors(
            colors.background, colors.surface1, colors.surface2, colors.surface3, colors.divider, colors.outline,
            colors.onSurface, colors.onSurfaceVariant, dyn.primary, dyn.onPrimary, colors.error, colors.onError,
            colors.protein, colors.carbs, colors.fat,
        )
        scheme = scheme.copy(primary = dyn.primary, onPrimary = dyn.onPrimary, secondary = dyn.primary, tertiary = dyn.primary)
    }
    CompositionLocalProvider(
        LocalTallyColors provides colors,
        LocalTallyTypography provides TallyNumberTypography,
        LocalReduceMotion provides rememberReduceMotion(),
    ) {
        MaterialTheme(colorScheme = scheme, typography = TallyM3Typography, shapes = TallyShapes, content = content)
    }
}

/** « Supprimer les animations » ⇒ ANIMATOR_DURATION_SCALE = 0. */
@Composable
private fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember { mutableStateOf(readReduced(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readReduced(resolver)
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

private fun readReduced(resolver: android.content.ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

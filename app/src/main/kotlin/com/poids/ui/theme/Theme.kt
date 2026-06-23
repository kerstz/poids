package com.poids.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Thème Material 3, direction « Sérénité ». Palette de marque fixe (pas de couleur
 * dynamique : l'accent bleu unique fait partie de l'identité). Expose les tokens
 * étendus via [LocalPoidsColors].
 */
@Composable
fun PoidsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalPoidsColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PoidsTypography,
            shapes = PoidsShapes,
            content = content,
        )
    }
}

/** Accès court aux tokens étendus : `PoidsTheme.extra.teal`. */
object PoidsTheme {
    val extra: PoidsExtraColors
        @Composable get() = LocalPoidsColors.current
}

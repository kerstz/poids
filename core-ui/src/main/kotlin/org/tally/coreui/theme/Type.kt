package org.tally.coreui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Styles numériques : chiffres tabulaires (docs/UI.md §2). */
@Immutable
class TallyTypography(
    val numberHero: TextStyle,
    val numberLarge: TextStyle,
    val numberBody: TextStyle,
)

private const val TNUM = "tnum"

private fun style(size: TextUnit, line: TextUnit, weight: FontWeight, letterSpacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = size,
        lineHeight = line,
        fontWeight = weight,
        letterSpacing = letterSpacing,
    )

internal val TallyNumberTypography = TallyTypography(
    numberHero = style(48.sp, 56.sp, FontWeight.Medium).copy(fontFeatureSettings = TNUM),
    numberLarge = style(28.sp, 36.sp, FontWeight.Medium).copy(fontFeatureSettings = TNUM),
    numberBody = style(16.sp, 24.sp, FontWeight.Normal).copy(fontFeatureSettings = TNUM),
)

internal val TallyM3Typography = Typography(
    titleLarge = style(22.sp, 28.sp, FontWeight.Medium),
    titleMedium = style(16.sp, 24.sp, FontWeight.SemiBold),
    bodyLarge = style(16.sp, 24.sp, FontWeight.Normal),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = style(12.sp, 16.sp, FontWeight.Medium, 0.4.sp),
    labelSmall = style(12.sp, 16.sp, FontWeight.Medium, 0.4.sp),
)

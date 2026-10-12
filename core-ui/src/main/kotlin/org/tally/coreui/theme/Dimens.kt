// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.coreui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Grille 4 dp exclusive (docs/UI.md §3). */
@Immutable
class TallySpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Marge horizontale d'écran. */
    val gutter: Dp = 16.dp,
)

@Immutable
class TallyDimens(
    val iconS: Dp = 20.dp,
    val iconM: Dp = 24.dp,
    val iconL: Dp = 48.dp,
    val touchTarget: Dp = 48.dp,
    val rowMinHeight: Dp = 56.dp,
    val divider: Dp = 1.dp,
    val macroBarHeight: Dp = 8.dp,
    val gaugeHeight: Dp = 8.dp,
    /** Réserve en bas de liste sous le FAB. */
    val fabClearance: Dp = 88.dp,
    val chartHeight: Dp = 220.dp,
)

internal val TallyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

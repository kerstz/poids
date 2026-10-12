// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.coreui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

/** Durées 150–250 ms (docs/UI.md §4). */
enum class TallyMotionToken(val millis: Int) {
    Short(150),
    Medium(200),
    Long(250),
}

private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Seul point d'entrée autorisé pour les animations : coupe tout si « réduire les animations ». */
@Composable
fun <T> tallyAnimationSpec(token: TallyMotionToken = TallyMotionToken.Short): FiniteAnimationSpec<T> =
    if (TallyTheme.reduceMotion) snap() else tween(token.millis, easing = StandardEasing)

// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.body

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.tally.body.weight.WeightScreen

@Serializable
data object BodyRoute

fun NavGraphBuilder.bodyGraph() {
    composable<BodyRoute> { WeightScreen() }
}

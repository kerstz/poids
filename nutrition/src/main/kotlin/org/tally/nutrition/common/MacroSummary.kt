// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.tally.domain.nutrition.Macros
import org.tally.nutrition.R

@Composable
internal fun MacroSummary(macros: Macros, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            R.string.macro_summary,
            formatNumber(macros.proteinG, 1),
            formatNumber(macros.carbsG, 1),
            formatNumber(macros.fatG, 1),
        ),
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

package org.tally.coreui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import org.tally.coreui.theme.TallyMotionToken
import org.tally.coreui.theme.TallyTheme
import org.tally.coreui.theme.tallyAnimationSpec

/** Textes déjà formatés (ex. « P 12 g »). Le libellé accompagne toujours la couleur. */
class MacroLegend(val protein: String, val carbs: String, val fat: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroBar(
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    modifier: Modifier = Modifier,
    legend: MacroLegend? = null,
) {
    val colors = TallyTheme.colors
    val total = (proteinG + carbsG + fatG).coerceAtLeast(0.0)
    val description = legend?.let { "${it.protein}, ${it.carbs}, ${it.fat}" }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { if (description != null) contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.s),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(TallyTheme.dimens.macroBarHeight)
                .clip(CircleShape)
                .background(colors.surface3),
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.dimens.divider),
        ) {
            if (total > 0.0) {
                listOf(proteinG to colors.protein, carbsG to colors.carbs, fatG to colors.fat)
                    .filter { it.first > 0.0 }
                    .forEach { (grams, color) ->
                        Box(Modifier.weight(grams.toFloat()).fillMaxHeight().background(color))
                    }
            }
        }
        if (legend != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs),
            ) {
                LegendItem(legend.protein, colors.protein)
                LegendItem(legend.carbs, colors.carbs)
                LegendItem(legend.fat, colors.fat)
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.xs)) {
        Box(Modifier.size(TallyTheme.spacing.s).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TallyTheme.colors.onSurfaceVariant)
    }
}

/** Progression vers un objectif ; accent unique. [label] est annoncé par TalkBack. */
@Composable
fun ProgressGauge(
    value: Float,
    target: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    val fraction = if (target > 0f) (value / target).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tallyAnimationSpec(TallyMotionToken.Medium), label = "gauge")
    Box(
        modifier
            .fillMaxWidth()
            .height(TallyTheme.dimens.gaugeHeight)
            .clip(CircleShape)
            .background(TallyTheme.colors.surface3)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(TallyTheme.colors.accent),
        )
    }
}

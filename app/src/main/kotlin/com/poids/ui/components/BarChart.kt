package com.poids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.poids.ui.theme.PoidsTheme

/**
 * Histogramme simple (Canvas-free, barres en Box). `values` et `labels` même taille.
 * Barres proportionnelles au max ; piste discrète sous chaque barre.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    modifier: Modifier = Modifier,
    barAreaHeightDp: Int = 90,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val frac = (value / max).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((barAreaHeightDp * frac).coerceAtLeast(2f).dp)
                        .background(
                            if (value <= 0f) PoidsTheme.extra.track else barColor,
                            RoundedCornerShape(4.dp),
                        ),
                )
                Text(
                    labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = PoidsTheme.extra.muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.coreui.chart

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import org.tally.coreui.theme.TallyTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Point d'une série temporelle : jour (epoch day) et valeur dans l'unité d'affichage. */
data class ChartPoint(val epochDay: Long, val value: Double)

/** Point d'intégration unique de Vico (voir docs/AUDIT.md §3.4). */
@Composable
fun TimeSeriesLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    /** Résumé annoncé par TalkBack (le graphe n'est pas navigable). */
    summary: String? = null,
) {
    val colors = TallyTheme.colors
    val vicoTheme = rememberM3VicoTheme(
        lineCartesianLayerColors = listOf(colors.accent),
        lineColor = colors.divider,
        textColor = colors.onSurfaceVariant,
    )
    // Plage serrée autour des données : sans cela l'axe part de 0 et la courbe paraît plate.
    val rangeProvider = remember(points) {
        val lo = points.minOfOrNull { it.value }
        val hi = points.maxOfOrNull { it.value }
        if (lo == null || hi == null) {
            CartesianLayerRangeProvider.auto()
        } else {
            val pad = maxOf(0.5, (hi - lo) * 0.2)
            CartesianLayerRangeProvider.fixed(minY = lo - pad, maxY = hi + pad)
        }
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    val dateFormatter = remember {
        val locale = Locale.getDefault()
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dM"), locale)
    }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            if (points.size >= 2) {
                lineModel {
                    series(x = points.map { it.epochDay }, y = points.map { it.value })
                }
            }
        }
    }

    ProvideVicoTheme(vicoTheme) {
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(rangeProvider = rangeProvider),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                guideline = null,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true),
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    LocalDate.ofEpochDay(value.toLong()).format(dateFormatter)
                },
            ),
        ),
        modelProducer = modelProducer,
        zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, minZoom = Zoom.Content),
        modifier = if (summary != null) {
            modifier.semantics { contentDescription = summary }
        } else {
            modifier
        },
    )
    }
}

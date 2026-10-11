package org.tally.coreui.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
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
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            if (points.size >= 2) {
                lineModel {
                    series(x = points.map { it.epochDay }, y = points.map { it.value })
                }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    LocalDate.ofEpochDay(value.toLong()).format(dateFormatter)
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

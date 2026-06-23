package com.poids.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Sparkline simple de la courbe de poids (Canvas, sans lib externe).
 * Vico arrivera à la feature ⑥ pour les graphiques riches.
 *
 * @param weightsChrono poids du plus ancien au plus récent.
 */
@Composable
fun WeightTrendChart(
    weightsChrono: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (weightsChrono.size < 2) return@Canvas

        val min = weightsChrono.min()
        val max = weightsChrono.max()
        val range = (max - min).let { if (it == 0.0) 1.0 else it }
        val padV = 12f
        val usableH = size.height - 2 * padV
        val stepX = size.width / (weightsChrono.size - 1)

        val points = weightsChrono.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = padV + ((max - value) / range * usableH).toFloat(),
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        points.forEach { drawCircle(color = lineColor, radius = 5f, center = it) }
    }
}

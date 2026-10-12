// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

import kotlin.math.cos

/** Énergie nette (au-dessus du repos), équations métaboliques ACSM (marche / course). */
object EstimateActivityEnergy {
    const val METHOD = "ACSM_NET_v1"
    private const val KCAL_PER_L_O2 = 5.0

    fun kcal(type: ActivityType, weightKg: Double, distanceM: Double, elevationGainM: Double): Double {
        val horizontal = if (type == ActivityType.RUN) 0.2 else 0.1
        val vertical = if (type == ActivityType.RUN) 0.9 else 1.8
        val mlO2PerKg = horizontal * distanceM + vertical * elevationGainM
        return weightKg * mlO2PerKg * KCAL_PER_L_O2 / 1000.0
    }
}

object Pace {
    private const val METERS_PER_MILE = 1609.344

    /** Secondes par km, null si distance ou durée nulle. */
    fun secPerKm(distanceM: Double, movingMs: Long): Double? =
        if (distanceM <= 0.0 || movingMs <= 0) null else movingMs / 1000.0 / (distanceM / 1000.0)

    fun secPerMile(distanceM: Double, movingMs: Long): Double? =
        secPerKm(distanceM, movingMs)?.let { it * METERS_PER_MILE / 1000.0 }

    /** Allure sur les 30 dernières secondes de points gardés (même segment), null si insuffisant. */
    fun currentSecPerKm(points: List<TrackPoint>, windowMs: Long = 30_000): Double? {
        val last = points.lastOrNull() ?: return null
        val window = points.filter { it.segment == last.segment && it.timeMs >= last.timeMs - windowMs }
        val first = window.firstOrNull() ?: return null
        if (first.seq == last.seq) return null
        val distance = window.zipWithNext().sumOf { (a, b) -> Geo.distanceM(a.lat, a.lon, b.lat, b.lon) }
        return secPerKm(distance, last.timeMs - first.timeMs)
    }
}

/** Simplification de tracé (Douglas-Peucker) pour l'affichage, sans récursion. */
object DouglasPeucker {
    data class LatLon(val lat: Double, val lon: Double)

    /** Indices conservés (croissants), tolérance `epsilonM` en mètres. */
    fun simplify(points: List<LatLon>, epsilonM: Double): List<Int> {
        val n = points.size
        if (n <= 2) return points.indices.toList()
        val lat0 = Math.toRadians(points.sumOf { it.lat } / n)
        val k = Geo.EARTH_RADIUS_M * Math.PI / 180.0
        val xs = DoubleArray(n) { points[it].lon * k * cos(lat0) }
        val ys = DoubleArray(n) { points[it].lat * k }
        val keep = BooleanArray(n)
        keep[0] = true
        keep[n - 1] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(0 to n - 1)
        while (stack.isNotEmpty()) {
            val (a, b) = stack.removeLast()
            var maxD = -1.0
            var maxI = -1
            for (i in a + 1 until b) {
                val d = perpendicular(xs[i], ys[i], xs[a], ys[a], xs[b], ys[b])
                if (d > maxD) { maxD = d; maxI = i }
            }
            if (maxI >= 0 && maxD > epsilonM) {
                keep[maxI] = true
                stack.add(a to maxI)
                stack.add(maxI to b)
            }
        }
        return keep.indices.filter { keep[it] }
    }

    /** Augmente epsilon jusqu'à ne garder que `maxPoints` points au plus. */
    fun simplifyTo(points: List<LatLon>, maxPoints: Int): List<Int> {
        var epsilon = 1.0
        var kept = simplify(points, epsilon)
        while (kept.size > maxPoints && epsilon < 1e6) {
            epsilon *= 1.5
            kept = simplify(points, epsilon)
        }
        return kept
    }

    private fun perpendicular(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        if (len2 == 0.0) return Math.hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

/** Seuils de filtrage (docs/SPORT-ARCHI.md §3.4). À calibrer sur traces réelles (jalon S5). */
data class TrackFilterConfig(
    val maxAccuracyM: Double = 25.0,
    val startAccuracyM: Double = 15.0,
    val minDistanceM: Double,
    val maxSpeedMps: Double,
    val elevationHysteresisM: Double = 10.0,
) {
    companion object {
        fun forType(type: ActivityType): TrackFilterConfig = when (type) {
            ActivityType.WALK -> TrackFilterConfig(minDistanceM = 5.0, maxSpeedMps = 4.0)
            ActivityType.RUN -> TrackFilterConfig(minDistanceM = 10.0, maxSpeedMps = 9.0)
        }
    }
}

/**
 * Filtre incrémental de fixes GPS. Garde un fix s'il est assez précis, assez loin du dernier
 * point gardé, dans l'ordre du temps et à une vitesse implicite plausible.
 */
class TrackFilter(private val config: TrackFilterConfig) {
    private var last: GpsFix? = null

    /** À appeler à la reprise après pause : le prochain fix démarre un nouveau segment. */
    fun reset() {
        last = null
    }

    fun accept(fix: GpsFix): Boolean {
        if (fix.accuracyM > config.maxAccuracyM) return false
        val previous = last
        if (previous == null) {
            if (fix.accuracyM > config.startAccuracyM) return false
            last = fix
            return true
        }
        val dtMs = fix.timeMs - previous.timeMs
        if (dtMs <= 0) return false
        val distance = Geo.distanceM(previous.lat, previous.lon, fix.lat, fix.lon)
        if (distance < config.minDistanceM) return false
        val speed = distance / (dtMs / 1000.0)
        if (speed > config.maxSpeedMps) return false
        last = fix
        return true
    }
}

/** Cumul du dénivelé avec hystérésis : un changement ne compte qu'au-delà du seuil. */
class ElevationAccumulator(private val hysteresisM: Double) {
    private var reference: Double? = null
    var gainM: Double = 0.0
        private set
    var lossM: Double = 0.0
        private set

    fun add(altM: Double?) {
        if (altM == null) return
        val ref = reference
        if (ref == null) {
            reference = altM
            return
        }
        val diff = altM - ref
        if (diff >= hysteresisM) {
            gainM += diff
            reference = altM
        } else if (diff <= -hysteresisM) {
            lossM += -diff
            reference = altM
        }
    }
}

object TrackAggregator {
    /** Agrégats recalculés depuis les points stockés : distance intra-segment + dénivelé. */
    fun aggregate(points: List<TrackPoint>, config: TrackFilterConfig): SessionAggregates {
        var distance = 0.0
        val elevation = ElevationAccumulator(config.elevationHysteresisM)
        var previous: TrackPoint? = null
        for (point in points.sortedBy { it.seq }) {
            val prev = previous
            if (prev != null && prev.segment == point.segment) {
                distance += Geo.distanceM(prev.lat, prev.lon, point.lat, point.lon)
            }
            elevation.add(point.altM)
            previous = point
        }
        return SessionAggregates(distance, elevation.gainM, elevation.lossM)
    }
}

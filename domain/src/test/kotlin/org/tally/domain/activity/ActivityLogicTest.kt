// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityLogicTest {
    // 1° de latitude ≈ 111 195 m avec R = 6 371 008,8 m.
    private val degLatM = 111_195.08

    private fun fix(t: Long, lat: Double, lon: Double = 2.0, acc: Double = 5.0, alt: Double? = null) =
        GpsFix(timeMs = t, lat = lat, lon = lon, altM = alt, accuracyM = acc, speedMps = null)

    private fun point(seq: Int, lat: Double, t: Long = seq * 1000L, seg: Int = 0, alt: Double? = null) =
        TrackPoint(seq, seg, t, lat, 2.0, alt, 5.0, null)

    @Test
    fun haversineMatchesKnownDistance() {
        val d = Geo.distanceM(48.0, 2.0, 48.01, 2.0)
        assertEquals(degLatM / 100, d, degLatM / 100 * 0.001)
        assertEquals(0.0, Geo.distanceM(10.0, 10.0, 10.0, 10.0), 0.0)
    }

    @Test
    fun filterRejectsImpreciseFixAndWaitsForGoodStart() {
        val f = TrackFilter(TrackFilterConfig.forType(ActivityType.WALK))
        assertFalse(f.accept(fix(0, 48.0, acc = 40.0)))
        assertFalse(f.accept(fix(1000, 48.0, acc = 20.0))) // ≤ 25 mais > 15 au démarrage
        assertTrue(f.accept(fix(2000, 48.0, acc = 10.0)))
    }

    @Test
    fun filterAppliesMinDistanceAndMaxSpeed() {
        val f = TrackFilter(TrackFilterConfig.forType(ActivityType.WALK))
        assertTrue(f.accept(fix(0, 48.0)))
        assertFalse(f.accept(fix(1000, 48.00002)))          // ≈ 2 m < 5 m
        assertTrue(f.accept(fix(5000, 48.00007)))           // ≈ 7,8 m en 5 s
        assertFalse(f.accept(fix(6000, 48.001)))            // ≈ 103 m en 1 s : trop rapide
        assertFalse(f.accept(fix(5000, 48.002)))            // temps non croissant
    }

    @Test
    fun runUsesWiderMinDistance() {
        val f = TrackFilter(TrackFilterConfig.forType(ActivityType.RUN))
        assertTrue(f.accept(fix(0, 48.0)))
        assertFalse(f.accept(fix(5000, 48.00007)))          // 7,8 m < 10 m
        assertTrue(f.accept(fix(10_000, 48.00014)))
    }

    @Test
    fun resetStartsNewSegmentWithoutSpeedCheck() {
        val f = TrackFilter(TrackFilterConfig.forType(ActivityType.WALK))
        assertTrue(f.accept(fix(0, 48.0)))
        f.reset()
        assertTrue(f.accept(fix(1000, 48.01)))              // loin, mais premier point du segment
    }

    @Test
    fun elevationHysteresisIgnoresNoise() {
        val e = ElevationAccumulator(10.0)
        listOf(100.0, 104.0, 98.0, 103.0, 112.0, 125.0, 110.0).forEach { e.add(it) }
        assertEquals(25.0, e.gainM, 1e-9)   // 100 → 112 (+12) → 125 (+13)
        assertEquals(15.0, e.lossM, 1e-9)
    }

    @Test
    fun aggregateSumsWithinSegmentsOnly() {
        val pts = listOf(
            point(0, 48.0), point(1, 48.001), point(2, 48.002),
            point(3, 48.01, seg = 1), point(4, 48.011, seg = 1),
        )
        val a = TrackAggregator.aggregate(pts, TrackFilterConfig.forType(ActivityType.WALK))
        assertEquals(degLatM * 0.003, a.distanceM, degLatM * 0.003 * 0.001)
    }

    @Test
    fun energyMatchesAcsmOrdersOfMagnitude() {
        assertEquals(175.0, EstimateActivityEnergy.kcal(ActivityType.WALK, 70.0, 5000.0, 0.0), 1e-9)
        assertEquals(350.0, EstimateActivityEnergy.kcal(ActivityType.RUN, 70.0, 5000.0, 0.0), 1e-9)
        assertEquals(63.0, EstimateActivityEnergy.kcal(ActivityType.WALK, 70.0, 0.0, 100.0), 1e-9)
    }

    @Test
    fun paceFromDistanceAndTime() {
        assertEquals(360.0, Pace.secPerKm(5000.0, 1_800_000)!!, 1e-9)
        assertEquals(360.0 * 1.609344, Pace.secPerMile(5000.0, 1_800_000)!!, 1e-6)
        assertNull(Pace.secPerKm(0.0, 1000))
    }

    @Test
    fun currentPaceUsesLastThirtySeconds() {
        // 10 m/10 s ≈ 0,00009° lat ; allure ≈ 1000 s/km
        val step = 10.0 / degLatM
        val pts = (0..6).map { point(it, 48.0 + it * step, t = it * 10_000L) }
        val pace = Pace.currentSecPerKm(pts)
        assertNotNull(pace)
        assertEquals(1000.0, pace!!, 1.0)
        assertNull(Pace.currentSecPerKm(pts.take(1)))
    }

    @Test
    fun douglasPeuckerDropsCollinearKeepsCorner() {
        val line = (0..10).map { DouglasPeucker.LatLon(48.0 + it * 0.0001, 2.0) }
        assertEquals(listOf(0, 10), DouglasPeucker.simplify(line, 1.0))
        val corner = line + (1..10).map { DouglasPeucker.LatLon(48.001, 2.0 + it * 0.0001) }
        val kept = DouglasPeucker.simplify(corner, 1.0)
        assertEquals(listOf(0, 10, 20), kept)
        assertTrue(DouglasPeucker.simplifyTo(corner, 3).size <= 3)
    }

    @Test
    fun movingMsIncludesOpenInterval() {
        val s = ActivitySession(
            uuid = "u", type = ActivityType.WALK, status = SessionStatus.RECORDING, startedAt = 0,
            zoneId = "UTC", localDate = 0, activeSince = 1000, movingMs = 500, createdAt = 0, updatedAt = 0,
        )
        assertEquals(2500, s.movingMsAt(3000))
        assertEquals(500, s.copy(activeSince = null).movingMsAt(3000))
    }
}

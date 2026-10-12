// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.activity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.tally.core.time.AppClock
import org.tally.domain.body.GetWeightAt
import org.tally.domain.body.WeightEntry
import org.tally.domain.body.WeightRepository

class SessionUseCasesTest {
    private class Clock(var now: Long) : AppClock {
        override fun nowMillis() = now
    }

    private class FakeRepo : ActivityRepository {
        val sessions = linkedMapOf<Long, ActivitySession>()
        val pointsBy = mutableMapOf<Long, MutableList<TrackPoint>>()
        private var next = 1L
        override fun observeSessions(): Flow<List<ActivitySession>> = flowOf(sessions.values.toList())
        override fun observeSession(id: Long): Flow<ActivitySession?> = flowOf(sessions[id])
        override suspend fun activeSession() = sessions.values.firstOrNull { it.status != SessionStatus.FINISHED }
        override suspend fun get(id: Long) = sessions[id]
        override suspend fun points(sessionId: Long) = pointsBy[sessionId].orEmpty().toList()
        override suspend fun lastPoint(sessionId: Long) = pointsBy[sessionId]?.lastOrNull()
        override suspend fun create(session: ActivitySession): Long {
            val id = next++
            sessions[id] = session.copy(id = id)
            return id
        }
        override suspend fun update(session: ActivitySession) { sessions[session.id] = session }
        override suspend fun appendBatch(sessionId: Long, points: List<TrackPoint>, aggregates: SessionAggregates, now: Long) {
            pointsBy.getOrPut(sessionId) { mutableListOf() }.addAll(points)
        }
        override suspend fun delete(id: Long) { sessions.remove(id); pointsBy.remove(id) }
        override suspend fun restore(session: ActivitySession, points: List<TrackPoint>) {
            sessions[session.id] = session; pointsBy[session.id] = points.toMutableList()
        }
        override suspend fun finishedWithoutEnergy() =
            sessions.values.filter { it.status == SessionStatus.FINISHED && it.energyKcal == null }
    }

    private class FakeWeights(private val kg: Double?) : WeightRepository {
        override fun observeEntries(): Flow<List<WeightEntry>> = flowOf(emptyList())
        override suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String?) = error("unused")
        override suspend fun update(entry: WeightEntry) = error("unused")
        override suspend fun delete(id: Long) = error("unused")
        override suspend fun latestAtOrBefore(instant: Long) = kg?.let { WeightEntry(1, it, 0, "UTC") }
        override suspend fun firstAfter(instant: Long): WeightEntry? = null
    }

    private fun points() = listOf(
        TrackPoint(0, 0, 0, 48.0, 2.0, 100.0, 5.0, null),
        TrackPoint(1, 0, 60_000, 48.0 + 5000.0 / 111_195.08, 2.0, 100.0, 5.0, null),
    )

    @Test
    fun startPauseResumeFinishAccountsMovingTime() = runTest {
        val repo = FakeRepo()
        val clock = Clock(1_000)
        val id = StartSession(repo, clock)(ActivityType.WALK, "Europe/Paris", "u1")
        clock.now = 11_000
        PauseSession(repo, clock)(id)
        assertEquals(10_000, repo.get(id)!!.movingMs)
        assertNull(repo.get(id)!!.activeSince)
        clock.now = 50_000
        ResumeSession(repo, clock)(id)
        clock.now = 55_000
        repo.appendBatch(id, points(), SessionAggregates(0.0, 0.0, 0.0), clock.now)
        FinishSession(repo, GetWeightAt(FakeWeights(70.0)), clock)(id)
        val done = repo.get(id)!!
        assertEquals(SessionStatus.FINISHED, done.status)
        assertEquals(15_000L, done.movingMs)
        assertEquals(55_000L, done.endedAt)
        assertEquals(5000.0, done.distanceM, 5.0)
        assertEquals(175.0, done.energyKcal!!, 1.0)
        assertEquals(EstimateActivityEnergy.METHOD, done.energyMethod)
    }

    @Test
    fun finishWithoutWeightLeavesEnergyNullThenRecompute() = runTest {
        val repo = FakeRepo()
        val clock = Clock(0)
        val id = StartSession(repo, clock)(ActivityType.RUN, "UTC", "u2")
        repo.appendBatch(id, points(), SessionAggregates(0.0, 0.0, 0.0), 0)
        clock.now = 60_000
        FinishSession(repo, GetWeightAt(FakeWeights(null)), clock)(id)
        assertNull(repo.get(id)!!.energyKcal)
        RecomputeMissingEnergy(repo, GetWeightAt(FakeWeights(null)), clock)()
        assertNull(repo.get(id)!!.energyKcal)
        RecomputeMissingEnergy(repo, GetWeightAt(FakeWeights(70.0)), clock)()
        assertEquals(350.0, repo.get(id)!!.energyKcal!!, 1.0)
    }

    @Test
    fun secondActiveSessionIsRefused() = runTest {
        val repo = FakeRepo()
        val start = StartSession(repo, Clock(0))
        start(ActivityType.WALK, "UTC", "a")
        try {
            start(ActivityType.RUN, "UTC", "b")
            fail("attendu IllegalStateException")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun deleteThenRestoreKeepsPoints() = runTest {
        val repo = FakeRepo()
        val id = StartSession(repo, Clock(0))(ActivityType.WALK, "UTC", "a")
        repo.appendBatch(id, points(), SessionAggregates(0.0, 0.0, 0.0), 0)
        val (session, pts) = DeleteSession(repo)(id)!!
        assertTrue(repo.sessions.isEmpty())
        RestoreSession(repo)(session, pts)
        assertEquals(2, repo.points(id).size)
    }
}

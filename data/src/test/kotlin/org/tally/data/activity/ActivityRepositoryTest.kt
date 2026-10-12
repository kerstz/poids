// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.activity

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.data.db.TallyDatabase
import org.tally.domain.activity.ActivitySession
import org.tally.domain.activity.ActivityType
import org.tally.domain.activity.SessionAggregates
import org.tally.domain.activity.SessionStatus
import org.tally.domain.activity.TrackPoint

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActivityRepositoryTest {
    private lateinit var db: TallyDatabase
    private lateinit var repository: ActivityRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(TallyDatabase.seedCallback)
            .build()
        repository = ActivityRepositoryImpl(db.activityDao())
    }

    @After
    fun tearDown() = db.close()

    private fun session(uuid: String, startedAt: Long, status: SessionStatus = SessionStatus.RECORDING) =
        ActivitySession(
            uuid = uuid, type = ActivityType.WALK, status = status, startedAt = startedAt,
            zoneId = "UTC", localDate = 0, createdAt = startedAt, updatedAt = startedAt,
        )

    private fun points(from: Int, count: Int) = (from until from + count).map {
        TrackPoint(it, 0, it * 1000L, 48.0 + it * 1e-5, 2.0, 100.0, 5.0, 1.2)
    }

    @Test
    fun sessionsAreSortedMostRecentFirstAndActiveIsFound() = runTest {
        repository.create(session("a", 100, SessionStatus.FINISHED))
        val b = repository.create(session("b", 300))
        repository.create(session("c", 200, SessionStatus.FINISHED))
        assertEquals(listOf(300L, 200L, 100L), repository.observeSessions().first().map { it.startedAt })
        assertEquals(b, repository.activeSession()!!.id)
    }

    @Test
    fun appendBatchStoresPointsAndAggregatesTogether() = runTest {
        val id = repository.create(session("a", 100))
        repository.appendBatch(id, points(0, 20), SessionAggregates(123.0, 4.0, 2.0), now = 999)
        repository.appendBatch(id, points(20, 5), SessionAggregates(150.0, 5.0, 2.0), now = 1000)
        assertEquals(25, repository.points(id).size)
        assertEquals(24, repository.lastPoint(id)!!.seq)
        val s = repository.get(id)!!
        assertEquals(150.0, s.distanceM, 0.0)
        assertEquals(1000L, s.updatedAt)
    }

    @Test
    fun deleteCascadesToPointsAndRestoreBringsThemBack() = runTest {
        val id = repository.create(session("a", 100))
        repository.appendBatch(id, points(0, 10), SessionAggregates(1.0, 0.0, 0.0), 1)
        val saved = repository.get(id)!!
        val savedPoints = repository.points(id)
        repository.delete(id)
        assertNull(repository.get(id))
        assertEquals(0, db.activityDao().points(id).size)
        repository.restore(saved, savedPoints)
        assertNotNull(repository.get(id))
        assertEquals(10, repository.points(id).size)
    }

    @Test
    fun finishedWithoutEnergyFiltersByStatusAndEnergy() = runTest {
        repository.create(session("a", 100, SessionStatus.FINISHED))
        repository.create(session("b", 200, SessionStatus.FINISHED).copy(energyKcal = 50.0))
        repository.create(session("c", 300))
        assertEquals(listOf("a"), repository.finishedWithoutEnergy().map { it.uuid })
    }

    @Test
    fun hundredThousandPointsRoundTrip() = runTest {
        val id = repository.create(session("big", 0))
        val ms = measureTimeMillis {
            (0 until 100_000 step 1000).forEach { start ->
                repository.appendBatch(id, points(start, 1000), SessionAggregates(0.0, 0.0, 0.0), 1)
            }
        }
        println("S1 100k points insérés en $ms ms")
        assertEquals(100_000, repository.points(id).size)
    }
}

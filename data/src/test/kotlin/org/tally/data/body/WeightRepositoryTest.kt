// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.body

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.data.db.TallyDatabase
import org.tally.domain.body.GetWeightAt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeightRepositoryTest {
    private lateinit var db: TallyDatabase
    private lateinit var repository: WeightRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(TallyDatabase.seedCallback)
            .build()
        repository = WeightRepositoryImpl(db.measurementDao(), object : AppClock {
            override fun nowMillis() = 5_000L
        })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun entriesAreSortedMostRecentFirst() = runTest {
        repository.add(80.0, 100, "UTC")
        repository.add(79.0, 300, "UTC")
        repository.add(79.5, 200, "UTC")
        assertEquals(listOf(300L, 200L, 100L), repository.observeEntries().first().map { it.measuredAt })
    }

    @Test
    fun updateAndDelete() = runTest {
        val id = repository.add(80.0, 100, "UTC")
        val entry = repository.observeEntries().first().single()
        repository.update(entry.copy(valueKg = 78.4, note = "matin"))
        val updated = repository.observeEntries().first().single()
        assertEquals(78.4, updated.valueKg, 0.0)
        assertEquals("matin", updated.note)
        repository.delete(id)
        assertEquals(0, repository.observeEntries().first().size)
    }

    @Test
    fun weightAtUsesPreviousThenNextEntry() = runTest {
        repository.add(80.0, 1_000, "UTC")
        repository.add(78.0, 3_000, "UTC")
        val weightAt = GetWeightAt(repository)
        assertEquals(80.0, weightAt(2_000)!!, 0.0)
        assertEquals(80.0, weightAt(500)!!, 0.0)
        assertEquals(78.0, weightAt(9_000)!!, 0.0)
    }

    @Test
    fun noWeightMeansNull() = runTest {
        assertNull(GetWeightAt(repository)(1_000))
    }
}

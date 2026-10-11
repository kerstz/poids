package org.tally.domain.body

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWeightAtTest {
    private class FakeRepository(private val entries: List<WeightEntry>) : WeightRepository {
        override fun observeEntries(): Flow<List<WeightEntry>> = flowOf(entries.sortedByDescending { it.measuredAt })
        override suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String?) = error("unused")
        override suspend fun update(entry: WeightEntry) = error("unused")
        override suspend fun delete(id: Long) = error("unused")
        override suspend fun latestAtOrBefore(instant: Long) =
            entries.filter { it.measuredAt <= instant }.maxByOrNull { it.measuredAt }
        override suspend fun firstAfter(instant: Long) =
            entries.filter { it.measuredAt > instant }.minByOrNull { it.measuredAt }
    }

    private fun entry(id: Long, at: Long, kg: Double) = WeightEntry(id, kg, at, "UTC")

    @Test
    fun usesLatestEntryAtOrBefore() = runTest {
        val useCase = GetWeightAt(FakeRepository(listOf(entry(1, 100, 80.0), entry(2, 200, 79.0), entry(3, 300, 78.0))))
        assertEquals(79.0, useCase(250)!!, 0.0)
        assertEquals(79.0, useCase(200)!!, 0.0)
    }

    @Test
    fun fallsBackToFirstEntryAfter() = runTest {
        val useCase = GetWeightAt(FakeRepository(listOf(entry(1, 100, 80.0), entry(2, 200, 79.0))))
        assertEquals(80.0, useCase(50)!!, 0.0)
    }

    @Test
    fun nullWhenNoEntry() = runTest {
        assertNull(GetWeightAt(FakeRepository(emptyList()))(100))
    }

    @Test
    fun validationBounds() {
        assertTrue(WeightValidation.isValid(72.5))
        assertFalse(WeightValidation.isValid(5.0))
        assertFalse(WeightValidation.isValid(Double.NaN))
    }
}

class SaveWeightTest {
    private class Recording : WeightRepository {
        var adds = 0
        override fun observeEntries(): Flow<List<WeightEntry>> = flowOf(emptyList())
        override suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String?): Long { adds++; return 1 }
        override suspend fun update(entry: WeightEntry) = Unit
        override suspend fun delete(id: Long) = Unit
        override suspend fun latestAtOrBefore(instant: Long): WeightEntry? = null
        override suspend fun firstAfter(instant: Long): WeightEntry? = null
    }

    @Test
    fun outOfRangeIsRejectedAndNotWritten() = runTest {
        val repo = Recording()
        val useCase = SaveWeight(repo)
        assertNull(useCase.add(5.0, 1, "UTC"))
        assertNull(useCase.add(900.0, 1, "UTC"))
        assertFalse(useCase.update(WeightEntry(1, 1.0, 1, "UTC")))
        assertEquals(0, repo.adds)
        assertEquals(1L, useCase.add(72.0, 1, "UTC"))
    }
}

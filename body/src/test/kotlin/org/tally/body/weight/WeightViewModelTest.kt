// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tally.body.weight

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tally.core.time.AppClock
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.domain.body.SaveWeight
import org.tally.domain.body.WeightEntry
import org.tally.domain.body.WeightRepository
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class WeightViewModelTest {
    private class FakeWeightRepository : WeightRepository {
        val entries = MutableStateFlow<List<WeightEntry>>(emptyList())
        private var nextId = 1L
        override fun observeEntries(): Flow<List<WeightEntry>> = entries.map { l -> l.sortedByDescending { it.measuredAt } }
        override suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String?): Long {
            val id = nextId++
            entries.value = entries.value + WeightEntry(id, valueKg, measuredAt, zoneId, note)
            return id
        }
        override suspend fun update(entry: WeightEntry) {
            entries.value = entries.value.map { if (it.id == entry.id) entry else it }
        }
        override suspend fun delete(id: Long) {
            entries.value = entries.value.filterNot { it.id == id }
        }
        override suspend fun latestAtOrBefore(instant: Long) = entries.value.filter { it.measuredAt <= instant }.maxByOrNull { it.measuredAt }
        override suspend fun firstAfter(instant: Long) = entries.value.filter { it.measuredAt > instant }.minByOrNull { it.measuredAt }
    }

    private class FakeSettings(mass: MassUnit = MassUnit.KG) : SettingsRepository {
        override val unitPreferences: Flow<UnitPreferences> = MutableStateFlow(UnitPreferences(mass = mass))
        override val offEnabled: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setOffEnabled(enabled: Boolean) = Unit
        override suspend fun setMassUnit(unit: MassUnit) = Unit
        override suspend fun setLengthUnit(unit: LengthUnit) = Unit
        override suspend fun setDistanceUnit(unit: DistanceUnit) = Unit
        override suspend fun setEnergyUnit(unit: EnergyUnit) = Unit
    }

    private val clock = object : AppClock {
        override fun nowMillis() = 1_700_000_000_000L
    }
    private val repo = FakeWeightRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(mass: MassUnit = MassUnit.KG) = WeightViewModel(repo, SaveWeight(repo), FakeSettings(mass), clock)

    @Test
    fun `etat expose les pesees recentes d'abord et l'unite`() = runTest {
        repo.add(80.0, 1_000, "UTC")
        repo.add(79.0, 2_000, "UTC")
        val vm = viewModel(MassUnit.LB)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(listOf(2_000L, 1_000L), vm.uiState.value.entries.map { it.measuredAt })
        assertEquals(MassUnit.LB, vm.uiState.value.massUnit)
    }

    @Test
    fun `sauvegarde valide ajoute avec l'horloge par defaut`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertTrue(vm.save(null, 80.5, null))

        val e = repo.entries.value.single()
        assertEquals(80.5, e.valueKg, 0.0001)
        assertEquals(clock.nowMillis(), e.measuredAt)
    }

    @Test
    fun `valeur saisie en livres convertie en kg`() = runTest {
        val vm = viewModel(MassUnit.LB)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.save(null, 176.0, 5L))
        assertEquals(79.83, repo.entries.value.single().valueKg, 0.01)
    }

    @Test
    fun `valeur hors bornes refusee sans ecriture`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertFalse(vm.save(null, 5.0, null))
        assertFalse(vm.save(null, Double.NaN, null))
        assertTrue(repo.entries.value.isEmpty())
    }

    @Test
    fun `modification d'une pesee existante`() = runTest {
        val id = repo.add(80.0, 1_000, "UTC")
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertTrue(vm.save(id, 78.0, null))

        val e = repo.entries.value.single()
        assertEquals(78.0, e.valueKg, 0.0001)
        assertEquals(1_000L, e.measuredAt)
    }

    @Test
    fun `suppression puis annulation reinsere la pesee`() = runTest {
        val id = repo.add(80.0, 1_000, "UTC", "matin")
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val entry = vm.uiState.value.entries.single { it.id == id }

        vm.delete(entry)
        assertTrue(repo.entries.value.isEmpty())
        val deleted = vm.deleted.first()
        assertEquals(entry, deleted)

        vm.undo(deleted)
        val restored = repo.entries.value.single()
        assertEquals(80.0, restored.valueKg, 0.0001)
        assertEquals("matin", restored.note)
    }
}

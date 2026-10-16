// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tally.nutrition.journal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.tally.domain.nutrition.MealSlot
import org.tally.domain.nutrition.RestoreJournalEntry
import org.tally.nutrition.FakeFoodRepository
import org.tally.nutrition.FakeGoals
import org.tally.nutrition.FakeJournalRepository
import org.tally.nutrition.FakeMealSlots
import org.tally.nutrition.MainDispatcherRule
import org.tally.nutrition.entry

class JournalViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val journal = FakeJournalRepository()
    private val goals = FakeGoals()
    private val slots = FakeMealSlots(listOf(MealSlot(1, "breakfast", null, 0), MealSlot(2, "lunch", null, 1)))

    private fun viewModel() = JournalViewModel(journal, RestoreJournalEntry(journal, FakeFoodRepository()), goals, slots)

    @Test
    fun `etat charge les sections et le total du jour`() = runTest {
        val vm = viewModel()
        val day = vm.uiState.value.epochDay
        journal.add(entry("Pomme", day, slot = 1, kcal = 50.0))
        journal.add(entry("Pain", day, slot = 2, kcal = 250.0))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value
        assertEquals(2, state.sections.size)
        assertEquals(listOf("Pomme"), state.sections[0].items.map { it.name })
        assertEquals(300.0, state.total.energyKcal, 0.001)
    }

    @Test
    fun `jour precedent change le jour observe`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val day = vm.uiState.value.epochDay
        vm.previousDay()
        assertEquals(day - 1, vm.uiState.value.epochDay)
        vm.nextDay()
        assertEquals(day, vm.uiState.value.epochDay)
    }

    @Test
    fun `suppression puis annulation reinsere l'entree`() = runTest {
        val vm = viewModel()
        val day = vm.uiState.value.epochDay
        val id = journal.add(entry("Pomme", day))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val item = vm.uiState.value.sections.first().items.single()

        vm.delete(item)
        val deleted = vm.deleted.first()
        assertEquals("Pomme", deleted.name)
        assertTrue(journal.entries.value.isEmpty())

        vm.undo(deleted)
        assertEquals(1, journal.entries.value.size)
        assertEquals("Pomme", journal.entries.value.values.single().name)
        assertTrue(id !in journal.entries.value.keys)
    }

    @Test
    fun `echec de l'annulation expose une erreur sans planter`() = runTest {
        val vm = viewModel()
        val id = journal.add(entry("Pomme", vm.uiState.value.epochDay))
        val snapshot = journal.snapshot(id)
        assertNotNull(snapshot)
        journal.failAdd = true

        vm.undo(DeletedEntry("Pomme", snapshot!!))

        assertEquals(JournalError.UNDO_FAILED, vm.errors.first())
    }

    @Test
    fun `echec de la suppression expose une erreur et aucun evenement d'annulation`() = runTest {
        val vm = viewModel()
        val day = vm.uiState.value.epochDay
        journal.add(entry("Pomme", day))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val item = vm.uiState.value.sections.first().items.single()
        journal.failDelete = true

        vm.delete(item)

        assertEquals(JournalError.DELETE_FAILED, vm.errors.first())
        assertEquals(1, journal.entries.value.size)
    }

    @Test
    fun `objectif energetique applique a partir du jour affiche`() = runTest {
        val vm = viewModel()
        vm.setEnergyTarget(2000.0)
        assertEquals(2000.0 to vm.uiState.value.epochDay, goals.lastEnergy)
    }
}

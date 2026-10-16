// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tally.nutrition.add

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tally.domain.model.QuantityUnit
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.ImportCiqualFood
import org.tally.domain.nutrition.ImportRemoteFood
import org.tally.domain.nutrition.LogFood
import org.tally.domain.nutrition.LogRecipe
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.ResolveBarcode
import org.tally.domain.nutrition.SaveFood
import org.tally.nutrition.FakeCiqual
import org.tally.nutrition.FakeFoodRepository
import org.tally.nutrition.FakeJournalRepository
import org.tally.nutrition.FakeRecipeRepository
import org.tally.nutrition.FakeRemote
import org.tally.nutrition.FakeSettings
import org.tally.nutrition.FixedClock
import org.tally.nutrition.MainDispatcherRule

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AddEntryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val apple = Food(1, "Pomme", per100 = Macros(52.0, 0.3, 14.0, 0.2), barcode = "3017620422003")
    private val foods = FakeFoodRepository(listOf(FoodWithPortions(apple, emptyList()), FoodWithPortions(Food(2, "Poire", per100 = Macros(57.0)), emptyList())))
    private val recipes = FakeRecipeRepository(listOf(Recipe(9, "Compote de pomme", 2.0, emptyList())))
    private val journal = FakeJournalRepository()
    private val remote = FakeRemote()

    private fun viewModel(): AddEntryViewModel {
        val saveFood = SaveFood(foods)
        val clock = FixedClock()
        return AddEntryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("mealSlotId" to 3L, "epochDay" to 20_000L)),
            foodRepository = foods,
            recipeRepository = recipes,
            logFood = LogFood(journal, clock) { java.time.ZoneOffset.UTC },
            logRecipe = LogRecipe(journal, clock) { java.time.ZoneOffset.UTC },
            remote = remote,
            resolveBarcode = ResolveBarcode(foods, remote),
            importRemoteFood = ImportRemoteFood(foods, saveFood),
            ciqual = FakeCiqual(),
            importCiqualFood = ImportCiqualFood(foods, saveFood),
            settings = FakeSettings(),
        )
    }

    @Test
    fun `la recherche filtre aliments et recettes`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(2, vm.uiState.value.foods.size)

        vm.setQuery("pomme")

        assertEquals(listOf("Pomme"), vm.uiState.value.foods.map { it.name })
        assertEquals(listOf("Compote de pomme"), vm.uiState.value.recipes.map { it.name })
    }

    @Test
    fun `ajout d'un aliment ecrit dans le journal du creneau et du jour de la route`() = runTest {
        val vm = viewModel()
        var done = false

        vm.addFood(apple, 150.0, null, null) { done = true }

        assertTrue(done)
        val e = journal.entries.value.values.single()
        assertEquals(20_000L, e.epochDay)
        assertEquals(3L, e.mealSlotId)
        assertEquals(150.0, e.quantityBase, 0.0)
        assertEquals(78.0, e.macros.energyKcal, 0.001)
    }

    @Test
    fun `ajout d'une recette utilise des portions`() = runTest {
        val vm = viewModel()
        var done = false
        vm.addRecipe(Recipe(9, "Compote", 2.0, emptyList()), 1.5) { done = true }
        assertTrue(done)
        assertEquals(QuantityUnit.SERVING, journal.entries.value.values.single().quantityUnit)
    }

    @Test
    fun `scan d'un code local selectionne l'aliment`() = runTest {
        val vm = viewModel()
        vm.onScanned("3017620422003")
        assertEquals("Pomme", vm.selectedFood.value?.food?.name)
        assertNull(vm.scanOutcome.value)
        vm.clearSelection()
        assertNull(vm.selectedFood.value)
    }

    @Test
    fun `scan d'un code invalide ou hors ligne propose la saisie manuelle`() = runTest {
        val vm = viewModel()
        vm.onScanned("123")
        assertEquals(ScanOutcome.NotFound("123"), vm.scanOutcome.value)

        vm.onScanned("5449000000996") // GTIN valide absent en local, réseau indisponible
        assertEquals(ScanOutcome.Offline("5449000000996"), vm.scanOutcome.value)
        vm.dismissScanOutcome()
        assertNull(vm.scanOutcome.value)
    }
}

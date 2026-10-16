// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tally.nutrition.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeIngredient
import org.tally.domain.nutrition.SaveRecipe
import org.tally.nutrition.FakeFoodRepository
import org.tally.nutrition.FakeRecipeRepository
import org.tally.nutrition.MainDispatcherRule

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RecipeEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val rice = Food(1, "Riz", per100 = Macros(130.0, 2.7, 28.0, 0.3))
    private val foods = FakeFoodRepository(listOf(FoodWithPortions(rice, emptyList())))
    private val recipes = FakeRecipeRepository()

    private fun viewModel(recipeId: Long = 0L) =
        RecipeEditorViewModel(SavedStateHandle(mapOf("recipeId" to recipeId)), recipes, SaveRecipe(recipes), foods)

    @Test
    fun `apercu par portion suit les ingredients et le nombre de portions`() = runTest {
        val vm = viewModel()
        vm.addIngredient(rice, 200.0)
        vm.servings = "2"
        assertEquals(130.0, vm.perServing.energyKcal, 0.001)
        vm.servings = "0"
        assertEquals(Macros.ZERO, vm.perServing)
    }

    @Test
    fun `sauvegarde valide`() = runTest {
        val vm = viewModel()
        vm.name = "Riz nature"
        vm.servings = "2"
        vm.addIngredient(rice, 200.0)
        var done = false

        vm.save { done = true }

        assertTrue(done)
        assertFalse(vm.showError)
        assertEquals("Riz nature", recipes.saved.single().name)
    }

    @Test
    fun `sauvegarde sans ingredient ou sans nom est refusee`() = runTest {
        val vm = viewModel()
        vm.name = "Vide"
        vm.save {}
        assertTrue(vm.showError)

        val vm2 = viewModel()
        vm2.addIngredient(rice, 100.0)
        vm2.save {}
        assertTrue(vm2.showError)
        assertTrue(recipes.saved.isEmpty())
    }

    @Test
    fun `edition charge la recette et retire un ingredient hors bornes sans effet`() = runTest {
        val ingredient = RecipeIngredient(1, "Riz", 100.0, rice.per100)
        recipes.items.value = mapOf(5L to Recipe(5, "Pilaf", 4.0, listOf(ingredient)))
        val vm = viewModel(5)

        assertTrue(vm.isEditing)
        assertEquals("Pilaf", vm.name)
        assertEquals(1, vm.ingredients.size)
        vm.removeIngredient(3)
        assertEquals(1, vm.ingredients.size)
        vm.removeIngredient(0)
        assertTrue(vm.ingredients.isEmpty())
    }

    @Test
    fun `recherche d'aliments filtre les resultats`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.foodResults.collect {} }
        assertEquals(listOf("Riz"), vm.foodResults.value.map { it.name })
        vm.setFoodQuery("zzz")
        assertTrue(vm.foodResults.value.isEmpty())
    }
}

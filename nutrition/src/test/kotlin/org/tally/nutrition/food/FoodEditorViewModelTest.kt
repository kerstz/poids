// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.SaveFood
import org.tally.nutrition.FakeFoodRepository
import org.tally.nutrition.MainDispatcherRule

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FoodEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun viewModel(repo: FakeFoodRepository, foodId: Long = 0L) =
        FoodEditorViewModel(SavedStateHandle(mapOf("foodId" to foodId)), repo, SaveFood(repo))


    @Test
    fun `sauvegarde valide ecrit l'aliment et ses portions`() = runTest {
        val repo = FakeFoodRepository()
        val vm = viewModel(repo)
        vm.name = "Riz"
        vm.energy = "130,5"
        vm.protein = "2.7"
        vm.addPortion()
        vm.portions.single().updateLabel("bol")
        vm.portions.single().updateQuantity("150")
        var done = false

        vm.save { done = true }

        assertTrue(done)
        assertFalse(vm.showError)
        val (food, portions) = repo.saved.single()
        assertEquals("Riz", food.name)
        assertEquals(130.5, food.per100.energyKcal, 0.001)
        assertEquals(listOf(FoodPortion(label = "bol", quantityBase = 150.0, isDefault = true)), portions)
    }

    @Test
    fun `sauvegarde invalide affiche l'erreur sans rien ecrire`() = runTest {
        val repo = FakeFoodRepository()
        val vm = viewModel(repo)
        vm.name = "   "
        vm.energy = "100"
        var done = false

        vm.save { done = true }

        assertFalse(done)
        assertTrue(vm.showError)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `energie absente ou illisible est refusee`() = runTest {
        val repo = FakeFoodRepository()
        val vm = viewModel(repo)
        vm.name = "Riz"
        vm.energy = "abc"
        vm.save {}
        assertTrue(vm.showError)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `edition charge l'aliment existant puis l'archive`() = runTest {
        val repo = FakeFoodRepository(
            listOf(
                FoodWithPortions(
                    Food(7, "Lait", "Marque", per100 = Macros(46.0, 3.4, 4.8, 1.6)),
                    listOf(FoodPortion(1, "verre", 250.0, true)),
                ),
            ),
        )
        val vm = viewModel(repo, foodId = 7)

        assertTrue(vm.isEditing)
        assertEquals("Lait", vm.name)
        assertEquals("Marque", vm.brand)
        assertEquals("46.0".replace('.', '.'), vm.energy.replace(',', '.'))
        assertEquals("verre", vm.portions.single().label)

        var archived = false
        vm.archive { archived = true }
        assertTrue(archived)
        assertEquals(listOf(7L), repo.archived)
    }
}

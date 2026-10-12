// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.nutrition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.time.AppClock
import org.tally.data.db.TallyDatabase
import org.tally.data.db.entity.FoodEntity
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.LogFood
import org.tally.domain.nutrition.RestoreJournalEntry
import org.tally.domain.nutrition.LogRecipe
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutritionCalc
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeIngredient

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NutritionRepositoryTest {
    private lateinit var db: TallyDatabase
    private lateinit var foods: FoodRepositoryImpl
    private lateinit var recipes: RecipeRepositoryImpl
    private lateinit var journal: JournalRepositoryImpl
    private lateinit var goals: GoalRepositoryImpl
    private val clock = object : AppClock { override fun nowMillis() = 10_000L }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(TallyDatabase.seedCallback)
            .build()
        foods = FoodRepositoryImpl(db.foodDao(), clock)
        recipes = RecipeRepositoryImpl(db.recipeDao(), clock)
        journal = JournalRepositoryImpl(db.journalDao(), clock)
        goals = GoalRepositoryImpl(db.goalDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun saveOats(): Food {
        val id = foods.save(
            Food(name = "Flocons d'avoine", brand = "Bio", per100 = Macros(370.0, 13.0, 60.0, 7.0)),
            listOf(FoodPortion(label = "1 bol", quantityBase = 40.0, isDefault = true)),
        )
        return foods.get(id)!!.food
    }

    @Test
    fun searchIsCaseInsensitiveAndEscapesWildcards() = runTest {
        saveOats()
        foods.save(Food(name = "100% pur jus", per100 = Macros(40.0, 0.0, 10.0, 0.0)), emptyList())
        assertEquals(1, foods.search("AVOINE").first().size)
        assertEquals(1, foods.search("bio").first().size) // marque
        assertEquals(listOf("100% pur jus"), foods.search("%").first().map { it.name })
        assertEquals(2, foods.search("").first().size)
    }

    @Test
    fun updatePreservesColumnsAbsentFromDomain() = runTest {
        val id = db.foodDao().insert(
            FoodEntity(
                name = "Yaourt", source = FoodSource.OFF, baseUnit = BaseUnit.ML,
                densityGPerMl = 1.03, energyKcal = 60.0, proteinG = 4.0, carbsG = 5.0, fatG = 3.0,
                sugarsG = 4.5, satFatG = 2.0, fiberG = 0.5, saltG = 0.1, offLastModified = 1234L,
                createdAt = 1L, updatedAt = 1L,
            ),
        )
        val food = foods.get(id)!!.food
        foods.save(food.copy(name = "Yaourt nature"), emptyList())
        val e = db.foodDao().get(id)!!
        assertEquals("Yaourt nature", e.name)
        assertEquals(4.5, e.sugarsG!!, 0.0)
        assertEquals(2.0, e.satFatG!!, 0.0)
        assertEquals(0.5, e.fiberG!!, 0.0)
        assertEquals(0.1, e.saltG!!, 0.0)
        assertEquals(1.03, e.densityGPerMl!!, 0.0)
        assertEquals(1234L, e.offLastModified)
    }

    @Test
    fun insertingAnExistingBarcodeReusesTheRowInsteadOfCrashing() = runTest {
        val first = foods.save(Food(name = "A", barcode = "3017620422003", per100 = Macros(1.0)), emptyList())
        val second = foods.save(Food(name = "A bis", barcode = "3017620422003", per100 = Macros(1.0)), emptyList())
        assertEquals(first, second)
    }

    @Test
    fun saveAppliesArchivedFlagBothWays() = runTest {
        val food = saveOats()
        foods.archive(food.id)
        val archived = foods.get(food.id)!!
        assertTrue(archived.food.isArchived)
        // L'éditeur repart de l'état stocké : l'archivage est conservé.
        foods.save(archived.food.copy(name = "Avoine bis"), archived.portions)
        assertTrue(foods.get(food.id)!!.food.isArchived)
        foods.save(foods.get(food.id)!!.food.copy(isArchived = false), archived.portions)
        assertEquals(false, foods.get(food.id)!!.food.isArchived)
        assertEquals(1, foods.search("avoine").first().size)
    }

    @Test
    fun barcodeIsStoredAsGtin13AndFoundWithUpcA() = runTest {
        val id = foods.save(Food(name = "Soda", barcode = "036000291452", per100 = Macros(40.0)), emptyList())
        assertEquals("0036000291452", foods.get(id)!!.food.barcode)
        assertEquals(id, foods.findByBarcode("036000291452")!!.id)
        assertEquals(id, foods.findByBarcode("0036000291452")!!.id)
    }

    @Test
    fun renamingAFoodDoesNotRewriteJournalName() = runTest {
        val food = saveOats()
        val slot = journalSlotId()
        val entryId = LogFood(journal, clock)(20000, slot, food, 50.0)
        foods.save(food.copy(name = "Autre nom"), emptyList())
        val item = journal.observeDay(20000).first().single { it.id == entryId }
        assertEquals("Flocons d'avoine", item.name)
    }

    @Test
    fun restoreWithStalePortionDoesNotViolateForeignKey() = runTest {
        val food = saveOats()
        val portion = foods.get(food.id)!!.portions.single()
        val slot = journalSlotId()
        val id = LogFood(journal, clock)(20000, slot, food, 40.0, portion.id, 1.0)
        val snapshot = journal.snapshot(id)!!
        journal.delete(id)
        // L'édition recrée les portions : l'ancienne est supprimée.
        foods.save(food.copy(name = "Avoine"), listOf(FoodPortion(label = "2 c.s.", quantityBase = 20.0)))
        val restored = RestoreJournalEntry(journal, foods)(snapshot)
        val item = journal.observeDay(20000).first().single { it.id == restored }
        assertEquals("Flocons d'avoine", item.name)
        assertEquals(null, journal.snapshot(restored)!!.portionId)
    }

    private suspend fun journalSlotId(): Long = db.mealSlotDao().observeSlots().first().first().id

    @Test
    fun portionsRoundTripAndUpdateReplacesThem() = runTest {
        val food = saveOats()
        assertEquals(1, foods.get(food.id)!!.portions.size)
        foods.save(food.copy(name = "Avoine"), listOf(FoodPortion(label = "1 c.s.", quantityBase = 10.0)))
        val updated = foods.get(food.id)!!
        assertEquals("Avoine", updated.food.name)
        assertEquals(listOf("1 c.s."), updated.portions.map { it.label })
    }

    @Test
    fun archivedFoodLeavesSearchButKeepsJournalHistory() = runTest {
        val food = saveOats()
        LogFood(journal, clock)(epochDay = 20_000, mealSlotId = 1, food = food, quantityBase = 50.0)
        foods.archive(food.id)
        assertTrue(foods.search("avoine").first().isEmpty())
        val items = journal.observeDay(20_000).first()
        assertEquals(1, items.size)
        assertEquals("Flocons d'avoine", items.single().name)
        assertEquals(185.0, items.single().macros.energyKcal, 1e-9)
    }

    @Test
    fun editingFoodDoesNotRewriteJournalSnapshot() = runTest {
        val food = saveOats()
        LogFood(journal, clock)(20_000, 1, food, 100.0)
        foods.save(food.copy(per100 = Macros(500.0, 13.0, 60.0, 7.0)), emptyList())
        assertEquals(370.0, journal.observeDay(20_000).first().single().macros.energyKcal, 1e-9)
    }

    @Test
    fun dayTotalsSumSnapshots() = runTest {
        val food = saveOats()
        val log = LogFood(journal, clock)
        log(20_000, 1, food, 100.0)
        log(20_000, 2, food, 50.0)
        log(20_001, 1, food, 100.0)
        val total = NutritionCalc.total(journal.observeDay(20_000).first())
        assertEquals(555.0, total.energyKcal, 1e-9)
    }

    @Test
    fun recipeIsLoggedPerServingAndSurvivesFoodArchive() = runTest {
        val food = saveOats()
        val recipeId = recipes.save(
            Recipe(
                name = "Porridge",
                servings = 2.0,
                ingredients = listOf(RecipeIngredient(food.id, food.name, 80.0, food.per100)),
            ),
        )
        val recipe = recipes.get(recipeId)!!
        assertEquals(148.0, recipe.perServing.energyKcal, 1e-9)
        assertEquals(1, recipes.observeRecipes().first().size)

        LogRecipe(journal, clock)(20_000, 1, recipe, servings = 1.5)
        foods.archive(food.id)
        val item = journal.observeDay(20_000).first().single()
        assertTrue(item.isRecipe)
        assertEquals(org.tally.domain.model.QuantityUnit.SERVING, item.quantityUnit)
        assertEquals("Porridge", item.name)
        assertEquals(222.0, item.macros.energyKcal, 1e-9)
    }

    @Test
    fun energyTargetIsHistorised() = runTest {
        goals.setEnergyTarget(2000.0, fromEpochDay = 20_000)
        goals.setEnergyTarget(1800.0, fromEpochDay = 20_010)
        assertEquals(null, goals.observeTargets(19_999).first().energyKcal)
        assertEquals(2000.0, goals.observeTargets(20_005).first().energyKcal!!, 0.0)
        assertEquals(1800.0, goals.observeTargets(20_010).first().energyKcal!!, 0.0)
    }
}

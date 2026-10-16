// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.tally.domain.model.QuantityUnit
import org.tally.domain.nutrition.CiqualFood
import org.tally.domain.nutrition.CiqualRepository
import org.tally.domain.nutrition.DayTargets
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodRemoteSource
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.GoalRepository
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.JournalRepository
import org.tally.domain.nutrition.MealSlot
import org.tally.domain.nutrition.MealSlotRepository
import org.tally.domain.nutrition.NewJournalEntry
import org.tally.domain.nutrition.RemoteFood
import org.tally.domain.nutrition.RemoteResult
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeRepository
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences
import org.tally.core.time.AppClock
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit

/** Remplace `Dispatchers.Main` par un dispatcher immédiat (viewModelScope déterministe). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FixedClock(private val now: Long = 1_700_000_000_000L) : AppClock {
    override fun nowMillis() = now
}

class FakeFoodRepository(foods: List<FoodWithPortions> = emptyList()) : FoodRepository {
    val items = MutableStateFlow(foods.associateBy { it.food.id })
    val saved = mutableListOf<Pair<Food, List<FoodPortion>>>()
    val archived = mutableListOf<Long>()
    private var nextId = 100L

    override fun search(query: String): Flow<List<Food>> = items.map { map ->
        map.values.map { it.food }.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    override suspend fun get(id: Long) = items.value[id]
    override suspend fun findByBarcode(barcode: String) =
        items.value.values.map { it.food }.firstOrNull { it.barcode == barcode && !it.isArchived }

    override suspend fun findByBarcodeAny(barcode: String) =
        items.value.values.map { it.food }.firstOrNull { it.barcode == barcode }

    override suspend fun findByCiqualCode(code: Int) =
        items.value.values.map { it.food }.firstOrNull { it.ciqualCode == code }

    override suspend fun save(food: Food, portions: List<FoodPortion>): Long {
        val id = if (food.id == 0L) nextId++ else food.id
        saved += food.copy(id = id) to portions
        items.value = items.value + (id to FoodWithPortions(food.copy(id = id), portions))
        return id
    }

    override suspend fun archive(id: Long) {
        archived += id
    }
}

class FakeRecipeRepository(recipes: List<Recipe> = emptyList()) : RecipeRepository {
    val items = MutableStateFlow(recipes.associateBy { it.id })
    val saved = mutableListOf<Recipe>()
    val archived = mutableListOf<Long>()

    override fun observeRecipes(): Flow<List<Recipe>> = items.map { it.values.toList() }
    override suspend fun get(id: Long) = items.value[id]
    override suspend fun save(recipe: Recipe): Long {
        val id = if (recipe.id == 0L) 50L else recipe.id
        saved += recipe.copy(id = id)
        items.value = items.value + (id to recipe.copy(id = id))
        return id
    }

    override suspend fun archive(id: Long) {
        archived += id
    }
}

class FakeJournalRepository : JournalRepository {
    val entries = MutableStateFlow<Map<Long, NewJournalEntry>>(emptyMap())
    var failAdd = false
    var failDelete = false
    private var nextId = 1L

    override fun observeDay(epochDay: Long): Flow<List<JournalItem>> = entries.map { map ->
        map.filterValues { it.epochDay == epochDay }.map { (id, e) -> e.toItem(id) }
    }

    override suspend fun add(entry: NewJournalEntry): Long {
        if (failAdd) error("add failed")
        val id = nextId++
        entries.value = entries.value + (id to entry)
        return id
    }

    override suspend fun delete(id: Long) {
        if (failDelete) error("delete failed")
        entries.value = entries.value - id
    }

    override suspend fun snapshot(id: Long) = entries.value[id]

    private fun NewJournalEntry.toItem(id: Long) = JournalItem(
        id = id,
        mealSlotId = mealSlotId,
        name = name,
        quantityBase = quantityBase,
        quantityUnit = quantityUnit,
        isRecipe = recipeId != null,
        portionCount = portionCount,
        macros = macros,
        detail = detail,
    )
}

class FakeMealSlots(slots: List<MealSlot>) : MealSlotRepository {
    private val flow = MutableStateFlow(slots)
    override fun observeSlots(): Flow<List<MealSlot>> = flow
}

class FakeGoals : GoalRepository {
    val targets = MutableStateFlow(DayTargets())
    var lastEnergy: Pair<Double, Long>? = null
    override fun observeTargets(epochDay: Long): Flow<DayTargets> = targets
    override suspend fun setEnergyTarget(kcal: Double, fromEpochDay: Long) {
        lastEnergy = kcal to fromEpochDay
    }
}

class FakeCiqual : CiqualRepository {
    override suspend fun search(query: String): List<CiqualFood> = emptyList()
}

class FakeRemote(var result: RemoteResult<RemoteFood> = RemoteResult.Unavailable) : FoodRemoteSource {
    override suspend fun findByBarcode(barcode: String) = result
    override suspend fun search(query: String): RemoteResult<List<RemoteFood>> = RemoteResult.Unavailable
}

class FakeSettings(offEnabled: Boolean = true) : SettingsRepository {
    override val unitPreferences: Flow<UnitPreferences> = MutableStateFlow(UnitPreferences())
    override val offEnabled: Flow<Boolean> = MutableStateFlow(offEnabled)
    override suspend fun setOffEnabled(enabled: Boolean) = Unit
    override suspend fun setMassUnit(unit: MassUnit) = Unit
    override suspend fun setLengthUnit(unit: LengthUnit) = Unit
    override suspend fun setDistanceUnit(unit: DistanceUnit) = Unit
    override suspend fun setEnergyUnit(unit: EnergyUnit) = Unit
}

internal fun entry(name: String = "Pomme", epochDay: Long, slot: Long = 1, kcal: Double = 52.0) = NewJournalEntry(
    name = name,
    brand = null,
    epochDay = epochDay,
    mealSlotId = slot,
    foodId = 1,
    recipeId = null,
    portionId = null,
    portionCount = null,
    quantityBase = 100.0,
    quantityUnit = QuantityUnit.G,
    macros = org.tally.domain.nutrition.Macros(kcal, 0.3, 14.0, 0.2),
    loggedAt = 0,
    zoneId = "UTC",
)

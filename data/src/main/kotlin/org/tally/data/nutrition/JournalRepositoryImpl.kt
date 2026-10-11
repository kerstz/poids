package org.tally.data.nutrition

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.core.time.AppClock
import org.tally.data.db.dao.GoalDao
import org.tally.data.db.dao.JournalDao
import org.tally.data.db.dao.JournalRow
import org.tally.data.db.dao.MealSlotDao
import org.tally.data.db.entity.GoalEntity
import org.tally.data.db.entity.JournalEntryEntity
import org.tally.domain.model.GoalMode
import org.tally.domain.model.GoalType
import org.tally.domain.nutrition.DayTargets
import org.tally.domain.nutrition.GoalRepository
import org.tally.domain.nutrition.JournalItem
import org.tally.domain.nutrition.JournalRepository
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.MealSlot
import org.tally.domain.nutrition.MealSlotRepository
import org.tally.domain.nutrition.NewJournalEntry
import org.tally.domain.nutrition.NutrientDetail
import kotlinx.coroutines.flow.combine

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalDao,
    private val clock: AppClock,
) : JournalRepository {

    override fun observeDay(epochDay: Long): Flow<List<JournalItem>> =
        dao.observeDay(epochDay).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(entry: NewJournalEntry): Long {
        val now = clock.nowMillis()
        return dao.insert(
            JournalEntryEntity(
                name = entry.name,
                brand = entry.brand,
                localEpochDay = entry.epochDay,
                mealSlotId = entry.mealSlotId,
                foodId = entry.foodId,
                recipeId = entry.recipeId,
                portionId = entry.portionId,
                portionCount = entry.portionCount,
                quantityBase = entry.quantityBase,
                quantityUnit = entry.quantityUnit,
                energyKcal = entry.macros.energyKcal,
                proteinG = entry.macros.proteinG,
                carbsG = entry.macros.carbsG,
                fatG = entry.macros.fatG,
                sugarsG = entry.detail.sugarsG,
                satFatG = entry.detail.satFatG,
                fiberG = entry.detail.fiberG,
                saltG = entry.detail.saltG,
                loggedAt = entry.loggedAt,
                zoneId = entry.zoneId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun snapshot(id: Long): NewJournalEntry? = dao.get(id)?.let {
        NewJournalEntry(
            name = it.name,
            brand = it.brand,
            epochDay = it.localEpochDay,
            mealSlotId = it.mealSlotId,
            foodId = it.foodId,
            recipeId = it.recipeId,
            portionId = it.portionId,
            portionCount = it.portionCount,
            quantityBase = it.quantityBase,
            quantityUnit = it.quantityUnit,
            macros = Macros(it.energyKcal, it.proteinG, it.carbsG, it.fatG),
            detail = NutrientDetail(it.sugarsG, it.satFatG, it.fiberG, it.saltG),
            loggedAt = it.loggedAt,
            zoneId = it.zoneId,
        )
    }

    private fun JournalRow.toDomain() = JournalItem(
        id = id,
        mealSlotId = mealSlotId,
        name = name,
        quantityBase = quantityBase,
        quantityUnit = quantityUnit,
        isRecipe = recipeId != null,
        portionCount = portionCount,
        macros = Macros(energyKcal, proteinG, carbsG, fatG),
        detail = NutrientDetail(sugarsG, satFatG, fiberG, saltG),
    )
}

@Singleton
class MealSlotRepositoryImpl @Inject constructor(
    private val dao: MealSlotDao,
) : MealSlotRepository {
    override fun observeSlots(): Flow<List<MealSlot>> =
        dao.observeSlots().map { rows -> rows.map { MealSlot(it.id, it.builtinKey, it.name, it.position) } }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao,
    private val clock: AppClock,
) : GoalRepository {

    override fun observeTargets(epochDay: Long): Flow<DayTargets> =
        combine(
            dao.observeLatest(GoalType.ENERGY_KCAL, epochDay),
            dao.observeLatest(GoalType.PROTEIN_G, epochDay),
            dao.observeLatest(GoalType.CARBS_G, epochDay),
            dao.observeLatest(GoalType.FAT_G, epochDay),
        ) { energy, protein, carbs, fat ->
            DayTargets(energy?.value, protein?.value, carbs?.value, fat?.value)
        }

    override suspend fun setEnergyTarget(kcal: Double, fromEpochDay: Long) {
        dao.upsert(
            GoalEntity(
                type = GoalType.ENERGY_KCAL,
                value = kcal,
                mode = GoalMode.FIXED,
                validFromEpochDay = fromEpochDay,
                createdAt = clock.nowMillis(),
            ),
        )
    }
}

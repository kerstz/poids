package org.tally.data.nutrition

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.core.time.AppClock
import org.tally.data.db.dao.FoodDao
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.Macros

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodDao,
    private val clock: AppClock,
) : FoodRepository {

    override fun search(query: String): Flow<List<Food>> =
        dao.search(escapeLike(query.trim())).map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): FoodWithPortions? {
        val entity = dao.get(id) ?: return null
        val portions = dao.portionsFor(id).map {
            FoodPortion(it.id, it.label, it.quantityBase, it.isDefault)
        }
        return FoodWithPortions(entity.toDomain(), portions)
    }

    override suspend fun save(food: Food, portions: List<FoodPortion>): Long {
        val now = clock.nowMillis()
        val portionEntities = portions.map {
            FoodPortionEntity(foodId = food.id, label = it.label, quantityBase = it.quantityBase, isDefault = it.isDefault)
        }
        return if (food.id == 0L) {
            dao.insertWithPortions(food.toEntity(createdAt = now, updatedAt = now), portionEntities)
        } else {
            val existing = requireNotNull(dao.get(food.id)) { "aliment ${food.id} introuvable" }
            dao.updateWithPortions(
                food.toEntity(createdAt = existing.createdAt, updatedAt = now).copy(archivedAt = existing.archivedAt),
                portionEntities,
            )
            food.id
        }
    }

    override suspend fun archive(id: Long) = dao.archive(id, clock.nowMillis())

    private fun Food.toEntity(createdAt: Long, updatedAt: Long) = FoodEntity(
        id = id,
        name = name.trim(),
        brand = brand?.trim()?.ifEmpty { null },
        barcode = barcode,
        source = source,
        baseUnit = baseUnit,
        energyKcal = per100.energyKcal,
        proteinG = per100.proteinG,
        carbsG = per100.carbsG,
        fatG = per100.fatG,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun FoodEntity.toDomain() = Food(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        source = source,
        baseUnit = baseUnit,
        per100 = Macros(energyKcal, proteinG, carbsG, fatG),
    )

    private companion object {
        /** Échappe `%`, `_` et `\` pour un `LIKE ... ESCAPE '\'`. */
        fun escapeLike(raw: String): String =
            raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}

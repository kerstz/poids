// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.nutrition

import android.database.sqlite.SQLiteConstraintException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tally.core.barcode.Gtin
import org.tally.core.time.AppClock
import org.tally.data.db.dao.FoodDao
import org.tally.data.db.entity.FoodEntity
import org.tally.data.db.entity.FoodPortionEntity
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodRepository
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutrientDetail

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

    override suspend fun findByBarcode(barcode: String): Food? =
        dao.findByBarcode(Gtin.normalize(barcode))?.toDomain()

    override suspend fun findByBarcodeAny(barcode: String): Food? =
        dao.findByBarcodeAny(Gtin.normalize(barcode))?.toDomain()

    override suspend fun findByCiqualCode(code: Int): Food? = dao.findByCiqualCode(code)?.toDomain()

    override suspend fun save(food: Food, portions: List<FoodPortion>): Long {
        val now = clock.nowMillis()
        val portionEntities = portions.map {
            FoodPortionEntity(foodId = food.id, label = it.label, quantityBase = it.quantityBase, isDefault = it.isDefault)
        }
        return if (food.id == 0L) {
            try {
                val entity = food.toEntity(createdAt = now, updatedAt = now)
                dao.insertWithPortions(entity.copy(archivedAt = if (food.isArchived) now else null), portionEntities)
            } catch (e: SQLiteConstraintException) {
                // Import concurrent du même code-barres (index unique) : on réutilise la ligne gagnante.
                food.barcode?.let { dao.findByBarcodeAny(Gtin.normalize(it))?.id } ?: throw e
            }
        } else {
            val existing = requireNotNull(dao.get(food.id)) { "aliment ${food.id} introuvable" }
            dao.updateWithPortions(
                food.toEntity(createdAt = existing.createdAt, updatedAt = now).copy(
                    archivedAt = if (food.isArchived) existing.archivedAt ?: now else null,
                ),
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
        barcode = barcode?.let(Gtin::normalize),
        source = source,
        ciqualCode = ciqualCode,
        offLastModified = offLastModified,
        baseUnit = baseUnit,
        densityGPerMl = densityGPerMl,
        energyKcal = per100.energyKcal,
        proteinG = per100.proteinG,
        carbsG = per100.carbsG,
        fatG = per100.fatG,
        sugarsG = per100Detail.sugarsG,
        satFatG = per100Detail.satFatG,
        fiberG = per100Detail.fiberG,
        saltG = per100Detail.saltG,
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
        per100Detail = NutrientDetail(sugarsG, satFatG, fiberG, saltG),
        densityGPerMl = densityGPerMl,
        ciqualCode = ciqualCode,
        offLastModified = offLastModified,
        isArchived = archivedAt != null,
    )

    private companion object {
        /** Échappe `%`, `_` et `\` pour un `LIKE ... ESCAPE '\'`. */
        fun escapeLike(raw: String): String =
            raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}

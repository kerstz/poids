// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.model.BaseUnit
import org.tally.domain.model.FoodSource

/** Valeurs nutritionnelles pour 100 g (ou 100 ml selon `base_unit`). `barcode` unique (NULL multiples permis). */
@Entity(
    tableName = "food",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["ciqual_code"], unique = true),
        Index(value = ["archived_at", "name"]),
    ],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val source: FoodSource,
    @ColumnInfo(name = "ciqual_code") val ciqualCode: Int? = null,
    @ColumnInfo(name = "off_last_modified") val offLastModified: Long? = null,
    @ColumnInfo(name = "base_unit") val baseUnit: BaseUnit,
    @ColumnInfo(name = "density_g_per_ml") val densityGPerMl: Double? = null,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "sugars_g") val sugarsG: Double? = null,
    @ColumnInfo(name = "fat_g") val fatG: Double,
    @ColumnInfo(name = "sat_fat_g") val satFatG: Double? = null,
    @ColumnInfo(name = "fiber_g") val fiberG: Double? = null,
    @ColumnInfo(name = "salt_g") val saltG: Double? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "food_portion",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("food_id")],
)
data class FoodPortionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    val label: String,
    /** Quantité en g ou ml (selon `food.base_unit`). */
    @ColumnInfo(name = "quantity_base") val quantityBase: Double,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
)

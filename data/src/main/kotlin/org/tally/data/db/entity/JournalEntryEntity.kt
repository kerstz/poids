package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tally.domain.model.QuantityUnit

/**
 * Entrée de journal : référence (aliment XOR recette) + quantité résolue + snapshot nutriments.
 * Modifier un aliment ne réécrit jamais l'historique. Contrainte XOR validée par le use case.
 */
@Entity(
    tableName = "journal_entry",
    foreignKeys = [
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_slot_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FoodPortionEntity::class,
            parentColumns = ["id"],
            childColumns = ["portion_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["local_date", "meal_slot_id"]),
        Index("food_id"),
        Index("recipe_id"),
        Index("portion_id"),
        Index("meal_slot_id"),
    ],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "local_date") val localEpochDay: Long,
    @ColumnInfo(name = "meal_slot_id") val mealSlotId: Long,
    @ColumnInfo(name = "food_id") val foodId: Long? = null,
    @ColumnInfo(name = "recipe_id") val recipeId: Long? = null,
    @ColumnInfo(name = "portion_id") val portionId: Long? = null,
    @ColumnInfo(name = "portion_count") val portionCount: Double? = null,
    @ColumnInfo(name = "quantity_base") val quantityBase: Double,
    @ColumnInfo(name = "quantity_unit") val quantityUnit: QuantityUnit,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

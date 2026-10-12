// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Double,
    @ColumnInfo(name = "cooked_weight_g") val cookedWeightG: Double? = null,
    val note: String? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "recipe_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("recipe_id"), Index("food_id")],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "quantity_base") val quantityBase: Double,
    val position: Int,
)

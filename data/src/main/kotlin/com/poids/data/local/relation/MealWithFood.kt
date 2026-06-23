package com.poids.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.poids.data.local.entity.FoodItemEntity
import com.poids.data.local.entity.MealLogEntity

/** Une entrée de repas jointe à son aliment (one-to-one via foodItemId). */
data class MealWithFood(
    @Embedded val meal: MealLogEntity,
    @Relation(parentColumn = "foodItemId", entityColumn = "id")
    val food: FoodItemEntity,
)

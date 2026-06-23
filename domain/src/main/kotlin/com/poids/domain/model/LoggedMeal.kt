package com.poids.domain.model

import java.time.LocalDateTime

/** Un aliment consommé : entrée de journal reliée à son [FoodItem]. */
data class LoggedMeal(
    val id: Long,
    val dateTime: LocalDateTime,
    val mealType: MealType,
    val food: FoodItem,
    val quantityG: Double,
    val photoPath: String?,
) {
    val kcal: Double get() = food.kcalFor(quantityG)
    val proteinG: Double? get() = food.proteinFor(quantityG)
    val carbsG: Double? get() = food.carbsFor(quantityG)
    val fatG: Double? get() = food.fatFor(quantityG)
}

package com.poids.domain.model

/**
 * Aliment (produit OFF, entrée USDA ou saisie manuelle). Valeurs nutritionnelles
 * pour 100 g/ml ; les champs optionnels peuvent manquer (données OFF incomplètes).
 */
data class FoodItem(
    val id: Long = 0L,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    val source: FoodSource,
    val energyKcal100: Double,
    val carbs100: Double? = null,
    val sugars100: Double? = null,
    val fat100: Double? = null,
    val satFat100: Double? = null,
    val protein100: Double? = null,
    val salt100: Double? = null,
    val fiber100: Double? = null,
    val servingSizeG: Double? = null,
    val nutriScore: String? = null,
    val novaGroup: Int? = null,
) {
    /** Calories pour une quantité donnée (g). */
    fun kcalFor(quantityG: Double): Double = energyKcal100 * quantityG / 100.0

    /** Protéines (g) pour une quantité donnée. */
    fun proteinFor(quantityG: Double): Double? = protein100?.let { it * quantityG / 100.0 }

    /** Glucides (g) pour une quantité donnée. */
    fun carbsFor(quantityG: Double): Double? = carbs100?.let { it * quantityG / 100.0 }

    /** Lipides (g) pour une quantité donnée. */
    fun fatFor(quantityG: Double): Double? = fat100?.let { it * quantityG / 100.0 }
}

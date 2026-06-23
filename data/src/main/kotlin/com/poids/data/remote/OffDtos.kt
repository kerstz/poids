package com.poids.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Réponse de l'endpoint OFF `GET /api/v2/product/{barcode}.json`.
 * `status` = 1 si le produit existe, 0 sinon.
 */
@Serializable
data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("nutriscore_grade") val nutriScore: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    val nutriments: OffNutriments? = null,
)

/** Valeurs pour 100 g/ml. Clés OFF telles quelles. Tout est optionnel (données incomplètes). */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100: Double? = null,
    @SerialName("carbohydrates_100g") val carbs100: Double? = null,
    @SerialName("sugars_100g") val sugars100: Double? = null,
    @SerialName("fat_100g") val fat100: Double? = null,
    @SerialName("saturated-fat_100g") val satFat100: Double? = null,
    @SerialName("proteins_100g") val protein100: Double? = null,
    @SerialName("salt_100g") val salt100: Double? = null,
    @SerialName("fiber_100g") val fiber100: Double? = null,
)

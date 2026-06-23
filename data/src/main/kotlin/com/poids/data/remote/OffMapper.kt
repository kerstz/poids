package com.poids.data.remote

import com.poids.data.local.entity.FoodItemEntity
import com.poids.domain.model.FoodSource
import java.time.Instant

/** Convertit un produit OFF en entité cache. `barcode` est celui scanné/recherché. */
fun OffProduct.toEntity(barcode: String): FoodItemEntity {
    val n = nutriments
    val displayName = productName?.takeIf { it.isNotBlank() }
        ?: brands?.takeIf { it.isNotBlank() }
        ?: "Produit $barcode"
    return FoodItemEntity(
        barcode = barcode,
        name = displayName,
        brand = brands?.takeIf { it.isNotBlank() },
        source = FoodSource.OPEN_FOOD_FACTS,
        energyKcal100 = n?.energyKcal100 ?: 0.0,
        carbs100 = n?.carbs100,
        sugars100 = n?.sugars100,
        fat100 = n?.fat100,
        satFat100 = n?.satFat100,
        protein100 = n?.protein100,
        salt100 = n?.salt100,
        fiber100 = n?.fiber100,
        servingSizeG = null,
        nutriScore = nutriScore
            ?.takeIf { it.isNotBlank() && it != "unknown" && it != "not-applicable" }
            ?.uppercase(),
        novaGroup = novaGroup,
        lastFetchedAt = Instant.now(),
    )
}

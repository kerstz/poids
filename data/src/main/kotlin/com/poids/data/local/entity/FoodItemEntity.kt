package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poids.domain.model.FoodSource
import java.time.Instant

/**
 * Aliment dans la base locale. Sert de **cache write-through** pour les produits
 * Open Food Facts / USDA scannés ou recherchés, et stocke aussi les aliments saisis
 * manuellement. Valeurs nutritionnelles **pour 100 g/ml**.
 *
 * `barcode` est indexé unique : un re-scan retrouve le produit hors-ligne. SQLite
 * autorise plusieurs lignes à barcode NULL (aliments manuels).
 *
 * Recherche par nom : LIKE sur `name`/`brand` (cache modeste → pas besoin de FTS).
 */
@Entity(
    tableName = "food_item",
    indices = [Index(value = ["barcode"], unique = true)],
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val source: FoodSource,
    val energyKcal100: Double,
    val carbs100: Double?,
    val sugars100: Double?,
    val fat100: Double?,
    val satFat100: Double?,
    val protein100: Double?,
    val salt100: Double?,
    val fiber100: Double?,
    val servingSizeG: Double?,
    val nutriScore: String?,
    val novaGroup: Int?,
    val lastFetchedAt: Instant?,
)

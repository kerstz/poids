package com.poids.domain.repository

import com.poids.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

/** Résultat d'une recherche d'un produit par code-barres. */
sealed interface FoodLookupResult {
    data class Found(val food: FoodItem) : FoodLookupResult

    /** Code-barres inconnu d'Open Food Facts. */
    data object NotFound : FoodLookupResult

    /** Échec réseau / API (l'utilisateur est probablement hors-ligne). */
    data class Error(val message: String) : FoodLookupResult
}

interface FoodRepository {
    /**
     * Cherche un produit par code-barres : cache Room d'abord (offline), puis API
     * Open Food Facts en repli. Un produit trouvé via l'API est écrit en cache
     * (write-through) pour que le prochain scan fonctionne hors-ligne.
     */
    suspend fun lookupByBarcode(barcode: String): FoodLookupResult

    /** Recherche locale par nom/marque (cache + aliments manuels). */
    suspend fun search(query: String, limit: Int = 50): List<FoodItem>

    /** Enregistre un aliment saisi manuellement. Retourne son id. */
    suspend fun saveManual(food: FoodItem): Long

    fun observeById(id: Long): Flow<FoodItem?>
}

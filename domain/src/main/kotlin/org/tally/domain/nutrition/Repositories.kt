// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.nutrition

import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    /** Aliments non archivés dont le nom/la marque contient [query] (vide = tous), triés par nom. */
    fun search(query: String): Flow<List<Food>>
    suspend fun get(id: Long): FoodWithPortions?
    /** Aliment non archivé portant ce code-barres, s'il existe. */
    suspend fun findByBarcode(barcode: String): Food?
    /** Comme [findByBarcode] mais archivés inclus (`Food.isArchived`) : l'index code-barres est unique. */
    suspend fun findByBarcodeAny(barcode: String): Food?
    /** Aliment issu de cette entrée Ciqual, archivé inclus. */
    suspend fun findByCiqualCode(code: Int): Food?
    /** Insère ou met à jour ; applique `Food.isArchived` (désarchive si `false`). */
    suspend fun save(food: Food, portions: List<FoodPortion>): Long
    /** Archivage : l'aliment disparaît des recherches mais l'historique reste intact. */
    suspend fun archive(id: Long)
}

/** Table de référence Ciqual embarquée (lecture seule, hors sauvegarde). */
interface CiqualRepository {
    /** Entrées dont le nom contient [query] (sans casse ni accents), au plus 50. */
    suspend fun search(query: String): List<CiqualFood>
}

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
    suspend fun get(id: Long): Recipe?
    suspend fun save(recipe: Recipe): Long
    suspend fun archive(id: Long)
}

interface JournalRepository {
    fun observeDay(epochDay: Long): Flow<List<JournalItem>>
    suspend fun add(entry: NewJournalEntry): Long
    suspend fun delete(id: Long)
    /** Copie réinsérable de l'entrée [id] (pour « Annuler » après suppression) ; null si absente. */
    suspend fun snapshot(id: Long): NewJournalEntry? = null
}

interface MealSlotRepository {
    fun observeSlots(): Flow<List<MealSlot>>
}

interface GoalRepository {
    fun observeTargets(epochDay: Long): Flow<DayTargets>
    /** Objectif énergétique fixe applicable à partir de [fromEpochDay] (historisé). */
    suspend fun setEnergyTarget(kcal: Double, fromEpochDay: Long)
}

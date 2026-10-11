package org.tally.domain.nutrition

/**
 * Points d'entrée d'écriture validés. Toute source de données (saisie, import, restauration)
 * doit passer par ces use cases : les repositories n'effectuent aucune validation métier.
 */
class SaveFood(private val repository: FoodRepository) {
    /**
     * Retourne l'id, ou null si l'aliment ou une portion est invalide, ou si le code-barres appartient
     * déjà à un autre aliment, archivé ou non (rien n'est écrit).
     */
    suspend operator fun invoke(food: Food, portions: List<FoodPortion>): Long? {
        if (!FoodValidation.isValid(food)) return null
        food.barcode?.let { code ->
            val owner = repository.findByBarcodeAny(code)
            if (owner != null && owner.id != food.id) return null
        }
        if (portions.any { it.label.isBlank() || !it.quantityBase.isFinite() || it.quantityBase <= 0 }) return null
        return repository.save(food, portions)
    }
}

class SaveRecipe(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipe: Recipe): Long? {
        if (!RecipeValidation.isValid(recipe)) return null
        return repository.save(recipe)
    }
}

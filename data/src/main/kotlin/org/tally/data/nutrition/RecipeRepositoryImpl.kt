// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.nutrition

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.tally.core.time.AppClock
import org.tally.data.db.dao.IngredientRow
import org.tally.data.db.dao.RecipeDao
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.NutrientDetail
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RecipeIngredient
import org.tally.domain.nutrition.RecipeRepository

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val dao: RecipeDao,
    private val clock: AppClock,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> =
        combine(dao.observeRecipes(), dao.observeIngredientRows()) { recipes, rows ->
            val byRecipe = rows.groupBy { it.recipeId }
            recipes.map { r -> Recipe(r.id, r.name, r.servings, byRecipe[r.id].orEmpty().map { it.toDomain() }) }
        }

    override suspend fun get(id: Long): Recipe? {
        val entity = dao.get(id) ?: return null
        return Recipe(entity.id, entity.name, entity.servings, dao.ingredientRows(id).map { it.toDomain() })
    }

    override suspend fun save(recipe: Recipe): Long {
        val now = clock.nowMillis()
        val items = recipe.ingredients.mapIndexed { index, ing ->
            RecipeIngredientEntity(
                recipeId = recipe.id,
                foodId = ing.foodId,
                quantityBase = ing.quantityBase,
                position = index,
            )
        }
        return if (recipe.id == 0L) {
            val entity = RecipeEntity(
                name = recipe.name.trim(),
                servings = recipe.servings,
                createdAt = now,
                updatedAt = now,
            )
            dao.insertWithIngredients(entity, items)
        } else {
            val existing = requireNotNull(dao.get(recipe.id)) { "recette ${recipe.id} introuvable" }
            dao.updateWithIngredients(
                existing.copy(name = recipe.name.trim(), servings = recipe.servings, updatedAt = now),
                items,
            )
            recipe.id
        }
    }

    override suspend fun archive(id: Long) = dao.archive(id, clock.nowMillis())

    private fun IngredientRow.toDomain() =
        RecipeIngredient(
            foodId, foodName, quantityBase, Macros(energyKcal, proteinG, carbsG, fatG),
            NutrientDetail(sugarsG, satFatG, fiberG, saltG),
        )
}

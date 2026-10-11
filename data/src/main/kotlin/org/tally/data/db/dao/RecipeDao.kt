package org.tally.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tally.data.db.entity.RecipeEntity
import org.tally.data.db.entity.RecipeIngredientEntity

data class IngredientRow(
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "food_name") val foodName: String,
    @ColumnInfo(name = "quantity_base") val quantityBase: Double,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
)

private const val INGREDIENT_SELECT =
    "SELECT i.recipe_id, i.food_id, f.name AS food_name, i.quantity_base, " +
        "f.energy_kcal, f.protein_g, f.carbs_g, f.fat_g " +
        "FROM recipe_ingredient i JOIN food f ON f.id = i.food_id "

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query(INGREDIENT_SELECT + "JOIN recipe r ON r.id = i.recipe_id WHERE r.archived_at IS NULL ORDER BY i.recipe_id, i.position")
    fun observeIngredientRows(): Flow<List<IngredientRow>>

    @Query(INGREDIENT_SELECT + "WHERE i.recipe_id = :recipeId ORDER BY i.position")
    suspend fun ingredientRows(recipeId: Long): List<IngredientRow>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun get(id: Long): RecipeEntity?

    @Insert
    suspend fun insert(recipe: RecipeEntity): Long

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Insert
    suspend fun insertIngredients(items: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("UPDATE recipe SET archived_at = :at, updated_at = :at WHERE id = :id")
    suspend fun archive(id: Long, at: Long)

    @Transaction
    suspend fun insertWithIngredients(recipe: RecipeEntity, items: List<RecipeIngredientEntity>): Long {
        val id = insert(recipe)
        insertIngredients(items.map { it.copy(id = 0, recipeId = id) })
        return id
    }

    @Transaction
    suspend fun updateWithIngredients(recipe: RecipeEntity, items: List<RecipeIngredientEntity>) {
        update(recipe)
        deleteIngredients(recipe.id)
        insertIngredients(items.map { it.copy(id = 0, recipeId = recipe.id) })
    }
}

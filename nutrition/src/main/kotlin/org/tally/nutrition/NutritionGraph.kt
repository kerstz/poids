package org.tally.nutrition

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.tally.nutrition.add.AddEntryScreen
import org.tally.nutrition.food.FoodEditorScreen
import org.tally.nutrition.journal.JournalScreen
import org.tally.nutrition.recipe.RecipeEditorScreen

@Serializable
data object NutritionRoute

@Serializable
internal data class AddEntryRoute(val mealSlotId: Long, val epochDay: Long)

@Serializable
internal data class FoodEditorRoute(val foodId: Long = 0)

@Serializable
internal data class RecipeEditorRoute(val recipeId: Long = 0)

fun NavGraphBuilder.nutritionGraph(navController: NavController) {
    composable<NutritionRoute> {
        JournalScreen(
            onAddEntry = { slotId, day -> navController.navigate(AddEntryRoute(slotId, day)) },
        )
    }
    composable<AddEntryRoute> {
        AddEntryScreen(
            onDone = { navController.popBackStack() },
            onNewFood = { navController.navigate(FoodEditorRoute()) },
            onEditFood = { id -> navController.navigate(FoodEditorRoute(id)) },
            onNewRecipe = { navController.navigate(RecipeEditorRoute()) },
        )
    }
    composable<FoodEditorRoute> {
        FoodEditorScreen(onDone = { navController.popBackStack() })
    }
    composable<RecipeEditorRoute> {
        RecipeEditorScreen(onDone = { navController.popBackStack() })
    }
}

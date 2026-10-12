package org.tally.nutrition

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.tally.nutrition.add.AddEntryScreen
import org.tally.nutrition.food.FoodEditorScreen
import org.tally.nutrition.journal.JournalScreen
import org.tally.nutrition.recipe.RecipeEditorScreen
import org.tally.nutrition.scan.ScanScreen

@Serializable
data object NutritionRoute

@Serializable
internal data class AddEntryRoute(val mealSlotId: Long, val epochDay: Long)

@Serializable
internal data class FoodEditorRoute(val foodId: Long = 0, val barcode: String? = null)

@Serializable
internal data object ScanRoute

/** Clé du résultat de scan déposée dans le `SavedStateHandle` de l'écran Ajout. */
internal const val SCAN_RESULT_KEY = "scan_result"

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
            onNewFoodWithBarcode = { code -> navController.navigate(FoodEditorRoute(barcode = code)) },
            onScan = { navController.navigate(ScanRoute) },
            onEditFood = { id -> navController.navigate(FoodEditorRoute(id)) },
            onNewRecipe = { navController.navigate(RecipeEditorRoute()) },
        )
    }
    composable<ScanRoute> {
        ScanScreen(
            onResult = { code ->
                navController.previousBackStackEntry?.savedStateHandle?.set(SCAN_RESULT_KEY, code)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<FoodEditorRoute> {
        FoodEditorScreen(onDone = { navController.popBackStack() })
    }
    composable<RecipeEditorRoute> {
        RecipeEditorScreen(onDone = { navController.popBackStack() })
    }
}

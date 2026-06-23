package com.poids.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poids.ui.home.HomeScreen
import com.poids.ui.nutrition.NutritionScreen
import com.poids.ui.nutrition.addmeal.AddMealScreen
import com.poids.ui.nutrition.manual.ManualFoodScreen
import com.poids.ui.nutrition.search.FoodSearchScreen
import com.poids.ui.profile.ProfileScreen
import com.poids.ui.weight.WeightScreen

/** Destinations de navigation. Étendu au fil des features. */
object Destinations {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val WEIGHT = "weight"
    const val NUTRITION = "nutrition"
    const val FOOD_SEARCH = "food_search"
    const val MANUAL_FOOD = "manual_food"
    const val ADD_MEAL = "add_meal/{foodId}"

    fun addMeal(foodId: Long) = "add_meal/$foodId"
}

@Composable
fun PoidsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                onConfigureProfile = { navController.navigate(Destinations.PROFILE) },
                onEditProfile = { navController.navigate(Destinations.PROFILE) },
                onOpenWeight = { navController.navigate(Destinations.WEIGHT) },
                onOpenNutrition = { navController.navigate(Destinations.NUTRITION) },
            )
        }
        composable(Destinations.PROFILE) {
            ProfileScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.WEIGHT) {
            WeightScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.NUTRITION) {
            NutritionScreen(
                onBack = { navController.popBackStack() },
                onSearch = { navController.navigate(Destinations.FOOD_SEARCH) },
                onManual = { navController.navigate(Destinations.MANUAL_FOOD) },
                onAddMeal = { foodId -> navController.navigate(Destinations.addMeal(foodId)) },
            )
        }
        composable(Destinations.FOOD_SEARCH) {
            FoodSearchScreen(
                onBack = { navController.popBackStack() },
                onSelectFood = { foodId -> navController.navigate(Destinations.addMeal(foodId)) },
            )
        }
        composable(Destinations.MANUAL_FOOD) {
            ManualFoodScreen(
                onBack = { navController.popBackStack() },
                onSaved = { foodId ->
                    navController.navigate(Destinations.addMeal(foodId)) {
                        popUpTo(Destinations.MANUAL_FOOD) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Destinations.ADD_MEAL,
            arguments = listOf(navArgument("foodId") { type = NavType.LongType }),
        ) {
            AddMealScreen(
                onDone = { navController.popBackStack(Destinations.NUTRITION, inclusive = false) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

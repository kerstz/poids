package com.poids.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.poids.R
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poids.ui.activity.ActivityScreen
import com.poids.ui.habits.HabitsScreen
import com.poids.ui.habits.SubstancesHistoryScreen
import com.poids.ui.home.HomeScreen
import com.poids.ui.nutrition.NutritionScreen
import com.poids.ui.nutrition.addmeal.AddMealScreen
import com.poids.ui.nutrition.manual.ManualFoodScreen
import com.poids.ui.nutrition.search.FoodSearchScreen
import com.poids.ui.profile.ProfileScreen
import com.poids.ui.profile.ProfileViewScreen
import com.poids.ui.trends.TrendsScreen
import com.poids.ui.weight.WeightScreen

object Destinations {
    // Onglets (barre de navigation)
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val NUTRITION = "nutrition"
    const val HABITS = "habits"
    const val PROFILE = "profile"

    // Détails (poussés par-dessus)
    const val PROFILE_EDIT = "profile_edit"
    const val WEIGHT = "weight"
    const val TRENDS = "trends"
    const val SUBSTANCES = "substances"
    const val FOOD_SEARCH = "food_search"
    const val MANUAL_FOOD = "manual_food"
    const val ADD_MEAL = "add_meal/{foodId}"

    fun addMeal(foodId: Long) = "add_meal/$foodId"
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Destinations.HOME, R.string.tab_summary, Icons.Filled.Home),
    Tab(Destinations.ACTIVITY, R.string.tab_activity, Icons.Filled.DirectionsRun),
    Tab(Destinations.NUTRITION, R.string.tab_nutrition, Icons.Filled.Restaurant),
    Tab(Destinations.HABITS, R.string.tab_habits, Icons.Filled.Bedtime),
    Tab(Destinations.PROFILE, R.string.tab_profile, Icons.Filled.Person),
)

private val topLevelRoutes = tabs.map { it.route }.toSet()

@Composable
fun PoidsNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Destinations.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    onConfigureProfile = { navController.navigate(Destinations.PROFILE_EDIT) },
                    onOpenWeight = { navController.navigate(Destinations.WEIGHT) },
                )
            }
            composable(Destinations.ACTIVITY) { ActivityScreen() }
            composable(Destinations.NUTRITION) {
                NutritionScreen(
                    onSearch = { navController.navigate(Destinations.FOOD_SEARCH) },
                    onManual = { navController.navigate(Destinations.MANUAL_FOOD) },
                    onAddMeal = { foodId -> navController.navigate(Destinations.addMeal(foodId)) },
                )
            }
            composable(Destinations.HABITS) {
                HabitsScreen(onOpenSubstances = { navController.navigate(Destinations.SUBSTANCES) })
            }
            composable(Destinations.PROFILE) {
                ProfileViewScreen(
                    onEdit = { navController.navigate(Destinations.PROFILE_EDIT) },
                    onOpenWeight = { navController.navigate(Destinations.WEIGHT) },
                    onOpenTrends = { navController.navigate(Destinations.TRENDS) },
                )
            }

            composable(Destinations.PROFILE_EDIT) {
                ProfileScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destinations.WEIGHT) {
                WeightScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.TRENDS) {
                TrendsScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.SUBSTANCES) {
                SubstancesHistoryScreen(onBack = { navController.popBackStack() })
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
}

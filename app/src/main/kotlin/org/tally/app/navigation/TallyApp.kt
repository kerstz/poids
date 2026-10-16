// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.tally.activity.activityGraph
import org.tally.body.bodyGraph
import org.tally.app.settings.PrivacyScreen
import org.tally.app.settings.SettingsScreen
import org.tally.nutrition.NutritionRoute
import org.tally.nutrition.nutritionGraph

@Composable
fun TallyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.visible.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(destination.route::class) == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NutritionRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            nutritionGraph(navController)
            bodyGraph()
            activityGraph()
            composable<SettingsRoute> { SettingsScreen(onOpenPrivacy = { navController.navigate(PrivacyRoute) }) }
            composable<PrivacyRoute> { PrivacyScreen() }
        }
    }
}

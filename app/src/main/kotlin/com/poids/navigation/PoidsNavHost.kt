package com.poids.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poids.ui.home.HomeScreen

/** Destinations de navigation. Étendu au fil des features. */
object Destinations {
    const val HOME = "home"
}

@Composable
fun PoidsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) { HomeScreen() }
    }
}

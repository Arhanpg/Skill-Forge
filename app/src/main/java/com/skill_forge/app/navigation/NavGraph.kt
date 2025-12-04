package com.skill_forge.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skill_forge.app.ui.auth.AuthScreen
import com.skill_forge.app.ui.main.MainScreen
import com.skill_forge.app.ui.main.screens.HomeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    homeViewModel: HomeViewModel // <--- Added Parameter
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(navController = navController)
        }
        composable("main") {
            // Pass the ViewModel to MainScreen
            MainScreen(
                navController = navController,
                homeViewModel = homeViewModel
            )
        }
    }
}
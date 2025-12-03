package com.skill_forge.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skill_forge.app.ui.auth.AuthScreen
import com.skill_forge.app.ui.main.MainScreen
import com.skill_forge.app.ui.main.MainScreen

@Composable
fun NavGraph(navController: NavHostController,startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(navController = navController)
        }
        composable("main") {
            MainScreen(navController = navController)
        }
    }
}

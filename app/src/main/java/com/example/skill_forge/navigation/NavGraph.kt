package com.example.skill_forge.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.skill_forge.ui.auth.AuthScreen
import com.example.skill_forge.ui.main.MainScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(navController = navController)
        }
        composable("main") {
            MainScreen()
        }
    }
}

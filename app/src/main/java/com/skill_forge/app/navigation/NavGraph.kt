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

/*

Copyright 2025 A^3*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at*
http://www.apache.org/licenses/LICENSE-2.0*
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
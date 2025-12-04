package com.skill_forge.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

// --- IMPORTS ---
// Ensure this package matches where your BottomNavigationBar file is located
import com.skill_forge.ui.main.components.BottomNavigationBar

import com.skill_forge.app.ui.main.screens.HomeScreen
import com.skill_forge.app.ui.main.screens.HomeViewModel
import com.skill_forge.app.ui.main.screens.ProfileScreen
import com.skill_forge.app.ui.main.screens.SkillTreeScreen
import com.skill_forge.app.ui.main.screens.StoreScreen
import com.skill_forge.app.ui.main.screens.TaskScreen

@Composable
fun MainScreen(
    navController: NavHostController, // Parent navController (for auth logout etc)
    homeViewModel: HomeViewModel      // ViewModel passed from MainActivity -> NavGraph
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2027),
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        )
                    )
                ),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    // FIX: Pass the homeViewModel here!
                    0 -> HomeScreen(viewModel = homeViewModel)
                    1 -> TaskScreen()
                    2 -> SkillTreeScreen()
                    3 -> StoreScreen()
                    4 -> ProfileScreen(auth, navController)
                }
            }
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
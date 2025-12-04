package com.skill_forge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.skill_forge.app.navigation.NavGraph
import com.skill_forge.app.ui.main.screens.HomeViewModel
import com.skill_forge.app.ui.theme.SkillForgeTheme

class MainActivity : ComponentActivity() {

    // 1. Initialize HomeViewModel here so we can access it in lifecycle methods
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Check Auth Status
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val startDestination = if (currentUser != null) "main" else "auth"

        setContent {
            SkillForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 2. Pass the viewModel to NavGraph
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }

    // 3. Lifecycle Hooks for Distraction Timer
    override fun onPause() {
        super.onPause()
        // When user leaves the app (minimize/switch apps), record timestamp
        homeViewModel.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        // When user comes back, calculate difference and add to distraction time
        homeViewModel.onAppForegrounded()
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
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
import com.skill_forge.app.ui.main.components.BottomNavigationBar
import com.skill_forge.app.ui.main.screens.HomeScreen
import com.skill_forge.app.ui.main.screens.ProfileScreen
import com.skill_forge.app.ui.main.screens.StoreScreen
import com.skill_forge.app.ui.main.screens.TaskScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScreen(navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // We still need auth for ProfileScreen if you haven't refactored that one yet
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
                    // FIX: Removed 'auth' parameter because HomeScreen now handles it internally
                    0 -> HomeScreen()
                    1 -> TaskScreen()
                    2 -> StoreScreen()
                    // ProfileScreen likely still needs these parameters based on your previous code
                    3 -> ProfileScreen(auth, navController)
                }
            }
        }
    }
}
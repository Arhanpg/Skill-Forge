package com.example.skill_forge.ui.main

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
import com.example.skill_forge.ui.main.components.BottomNavigationBar
import com.example.skill_forge.ui.main.screens.HomeScreen
import com.example.skill_forge.ui.main.screens.ProfileScreen
import com.example.skill_forge.ui.main.screens.StoreScreen

import com.example.skill_forge.ui.main.screens.TaskScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScreen(navController: NavHostController) {
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
                    0 -> HomeScreen(auth)
                    1 -> TaskScreen()
                    2 -> StoreScreen()
                    3 -> ProfileScreen(auth, navController)
                }
            }
        }
    }
}
package com.example.skill_forge.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScreen() {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to Skill Forge",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            user?.let {
                Text(
                    "Hello, ${it.displayName ?: it.email}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Cyan
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Your timer and hero progression UI goes here",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    auth.signOut()
                    // You can navigate back to auth screen here
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5555)
                )
            ) {
                Text("Sign Out", color = Color.White)
            }
        }
    }
}

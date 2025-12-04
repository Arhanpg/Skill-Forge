package com.skill_forge.app.ui.main.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.skill_forge.app.R // Ensure your R file is imported
import com.skill_forge.app.ui.main.models.UserProfile
import com.skill_forge.app.ui.main.components.AvatarPickerDialog
import com.skill_forge.app.ui.main.components.EditProfileDialog

@Composable
fun ProfileScreen(auth: FirebaseAuth, navController: NavHostController) {
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance("skillforge")
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Brand Colors
    val neonCyan = Color(0xFF00E5FF)
    val darkBg = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)

    // --- System Bar Coloring ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = darkBg.toArgb()
        }
    }

    // State
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    // --- RANK CALCULATION ---
    val currentRank = getRankFromXp(userProfile.xp)

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profile = document.toObject(UserProfile::class.java)
                        // Manual mapping to ensure XP/Coins are caught if Auto-map fails
                        val xp = document.getLong("xp")?.toInt() ?: 0
                        val coins = document.getLong("coins")?.toInt() ?: 0
                        if (profile != null) {
                            userProfile = profile.copy(xp = xp, coins = coins)
                        }
                    }
                    loading = false
                }
                .addOnFailureListener { loading = false }
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().background(darkBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = neonCyan)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(darkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER WITH AVATAR ---
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 60.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color(0xFF006064), darkBg)))
                )

                Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier.size(130.dp).shadow(10.dp, CircleShape).clip(CircleShape)
                            .border(4.dp, cardBg, CircleShape).border(6.dp, neonCyan, CircleShape)
                            .background(cardBg).clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !userProfile.avatarUri.isNullOrEmpty() -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(userProfile.avatarUri).crossfade(true).build(),
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            userProfile.avatarResourceId != null -> {
                                Image(
                                    painter = painterResource(id = userProfile.avatarResourceId!!),
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        }
                    }
                    Box(
                        modifier = Modifier.offset(x = (-4).dp, y = (-4).dp).size(40.dp).clip(CircleShape)
                            .background(neonCyan).border(3.dp, cardBg, CircleShape)
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- USERNAME ---
            Text(
                text = userProfile.username.ifEmpty { user?.displayName ?: "Skill Forger" },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.Bold
            )
            Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // --- RANK CARD UI ---
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700), // Gold color for trophy
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CURRENT RANK",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentRank.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userProfile.xp} XP Earned",
                            color = neonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Rank Image
                    Image(
                        painter = painterResource(id = currentRank.drawableId),
                        contentDescription = "Rank Badge",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- STATS CHIPS ---
            // Existing Chips (School/DOB) still use Icons as we don't have images for them yet
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (userProfile.qualification.isNotEmpty()) {
                    ProfileBadge(Icons.Default.School, userProfile.qualification, neonCyan, cardBg)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (userProfile.dob.isNotEmpty()) {
                    ProfileBadge(Icons.Default.Cake, userProfile.dob, Color(0xFFFFAB40), cardBg)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // UPDATED: Now using Custom Image for Coins
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ProfileBadge(R.drawable.coin_display_outline, "Coins: ${userProfile.coins}", Color.Yellow, cardBg)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- MENU ---
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Account Settings", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ProfileMenuItem(Icons.Default.Edit, "Edit Profile") { showEditDialog = true }
                        HorizontalDivider(color = darkBg, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(Icons.Default.Settings, "App Settings") { /* TODO */ }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LOGOUT BUTTON ---
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("auth") { popUpTo("main") { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f), contentColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- DIALOGS ---
    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentProfile = userProfile,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { selected ->
                userProfile = selected
                user?.uid?.let { uid ->
                    db.collection("users").document(uid).set(
                        mapOf("avatarResourceId" to selected.avatarResourceId, "avatarUri" to selected.avatarUri),
                        SetOptions.merge()
                    )
                }
                showAvatarPicker = false
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                userProfile = updated
                user?.uid?.let { uid ->
                    db.collection("users").document(uid).set(
                        mapOf("username" to updated.username, "dob" to updated.dob, "qualification" to updated.qualification),
                        SetOptions.merge()
                    )
                }
                showEditDialog = false
            }
        )
    }
}

// ===================== HELPERS & COMPONENTS =====================

// Original Vector Icon Version (Used for Qualification/DOB)
@Composable
fun ProfileBadge(icon: ImageVector, text: String, tint: Color, bgColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// NEW: Drawable Resource ID Version (Used for Coins/XP)
@Composable
fun ProfileBadge(iconRes: Int, text: String, tint: Color, bgColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Renders the full color image instead of a tinted icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
    }
}

// --- RANK DATA MODEL & LOGIC ---

data class RankDisplay(val title: String, val drawableId: Int)

fun getRankFromXp(xp: Int): RankDisplay {
    // Logic matching your HomeViewModel
    return when {
        xp >= 8000 -> RankDisplay("Master III", R.drawable.rank_master_3)
        xp >= 6000 -> RankDisplay("Master II", R.drawable.rank_master_2)
        xp >= 4000 -> RankDisplay("Master I", R.drawable.rank_master_1)
        xp >= 2500 -> RankDisplay("Gold III", R.drawable.rank_gold_3)
        xp >= 2000 -> RankDisplay("Gold II", R.drawable.rank_gold_2)
        xp >= 1500 -> RankDisplay("Gold I", R.drawable.rank_gold_1)
        xp >= 1000 -> RankDisplay("Silver III", R.drawable.rank_silver_3)
        xp >= 800 -> RankDisplay("Silver II", R.drawable.rank_silver_2)
        xp >= 600 -> RankDisplay("Silver I", R.drawable.rank_silver_1)
        xp >= 500 -> RankDisplay("Bronze III", R.drawable.rank_bronze_3)
        xp >= 400 -> RankDisplay("Bronze II", R.drawable.rank_bronze_2)
        xp >= 300 -> RankDisplay("Bronze I", R.drawable.rank_bronze_1)
        xp >= 200 -> RankDisplay("Wood III", R.drawable.rank_wood_3)
        xp >= 100 -> RankDisplay("Wood II", R.drawable.rank_wood_2)
        else -> RankDisplay("Wood I", R.drawable.rank_wood_1)
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
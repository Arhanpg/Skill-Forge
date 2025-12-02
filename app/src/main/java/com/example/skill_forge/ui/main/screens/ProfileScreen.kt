package com.example.skill_forge.ui.main.screens

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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
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
import com.example.skill_forge.ui.main.components.AvatarPickerDialog
import com.example.skill_forge.ui.main.components.EditProfileDialog
import com.example.skill_forge.ui.main.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun ProfileScreen(auth: FirebaseAuth, navController: NavHostController) {
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance() // Make sure this matches AuthScreen
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

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            // FIX: Changed "user_profiles" to "users" to match AuthScreen logic
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            // Firestore auto-mapping is safer if UserProfile has default values
                            val profile = document.toObject(UserProfile::class.java)
                            if (profile != null) {
                                userProfile = profile
                            }
                        } catch (e: Exception) {
                            // Fallback manual mapping if auto fails
                            userProfile = UserProfile(
                                username = document.getString("username") ?: "",
                                dob = document.getString("dob") ?: "",
                                qualification = document.getString("qualification") ?: "",
                                avatarResourceId = document.getLong("avatarResourceId")?.toInt(),
                                avatarUri = document.getString("avatarUri")
                            )
                        }
                    }
                    loading = false
                }
                .addOnFailureListener { loading = false }
        }
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(darkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = neonCyan)
        }
        return
    }

    // Main Container
    Box(modifier = Modifier.fillMaxSize().background(darkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER SECTION ---
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth()) {
                // Gradient Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 60.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color(0xFF006064), darkBg)))
                )

                // Avatar Container
                Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .border(4.dp, cardBg, CircleShape)
                            .border(6.dp, neonCyan, CircleShape)
                            .background(cardBg)
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            userProfile.avatarUri != null -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(userProfile.avatarUri)
                                        .crossfade(true)
                                        .build(),
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
                            else -> {
                                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            }
                        }
                    }

                    // Edit Badge
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(neonCyan)
                            .border(3.dp, cardBg, CircleShape)
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- USER INFO ---
            // FIX: Changed .name to .username
            Text(
                text = userProfile.username.ifEmpty { user?.displayName ?: "Skill Forger" },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // --- STATS ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userProfile.qualification.isNotEmpty()) {
                    ProfileBadge(Icons.Default.School, userProfile.qualification, neonCyan, cardBg)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (userProfile.dob.isNotEmpty()) {
                    ProfileBadge(Icons.Default.Cake, userProfile.dob, Color(0xFFFFAB40), cardBg)
                }
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
                        HorizontalDivider(color = darkBg, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuItem(Icons.AutoMirrored.Filled.Help, "Help & Support") { /* TODO */ }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LOGOUT ---
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("auth") { popUpTo("main") { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f), contentColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
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
            onAvatarSelected = { selectedProfile ->
                userProfile = selectedProfile
                user?.uid?.let { uid ->
                    // FIX: Changed "name" to "username" and collection to "users"
                    db.collection("users").document(uid).set(
                        mapOf(
                            "username" to userProfile.username,
                            "dob" to userProfile.dob,
                            "qualification" to userProfile.qualification,
                            "avatarResourceId" to selectedProfile.avatarResourceId,
                            "avatarUri" to selectedProfile.avatarUri
                        ), SetOptions.merge()
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
            onSave = { updatedProfile ->
                userProfile = updatedProfile
                user?.uid?.let { uid ->
                    // FIX: Changed "name" to "username" and collection to "users"
                    db.collection("users").document(uid).set(
                        mapOf(
                            "username" to updatedProfile.username,
                            "dob" to updatedProfile.dob,
                            "qualification" to updatedProfile.qualification,
                            "avatarResourceId" to updatedProfile.avatarResourceId,
                            "avatarUri" to updatedProfile.avatarUri
                        ), SetOptions.merge()
                    )
                }
                showEditDialog = false
            }
        )
    }
}

// --- HELPER COMPONENTS ---

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

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
    }
}
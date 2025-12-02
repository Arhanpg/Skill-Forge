package com.example.skill_forge.ui.main.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.skill_forge.ui.main.components.AvatarPickerDialog
import com.example.skill_forge.ui.main.components.EditProfileDialog
import com.example.skill_forge.ui.main.components.ProfileOption
import com.example.skill_forge.ui.main.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(auth: FirebaseAuth, navController: NavHostController) {
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("user_profiles").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userProfile = UserProfile(
                            name = document.getString("name") ?: "",
                            dob = document.getString("dob") ?: "",
                            qualification = document.getString("qualification") ?: "",
                            avatarResourceId = document.getLong("avatarResourceId")?.toInt(),
                            avatarUri = document.getString("avatarUri")
                        )
                    }
                    loading = false
                }
                .addOnFailureListener { loading = false }
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF00E5FF), CircleShape)
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
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF0097A7)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00BCD4))
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            userProfile.name.ifEmpty { user?.displayName ?: "Anonymous Hero" },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            user?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )

        if (userProfile.dob.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "🎂 ${userProfile.dob}",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp
            )
        }

        if (userProfile.qualification.isNotEmpty()) {
            Text(
                "🎓 ${userProfile.qualification}",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2632).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ProfileOption("Edit Profile", Icons.Default.Edit) { showEditDialog = true }
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                ProfileOption("Settings", Icons.Default.Settings) {}
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                ProfileOption("Help", Icons.AutoMirrored.Filled.Help) {}
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                auth.signOut()
                navController.navigate("auth") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontWeight = FontWeight.Bold)
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentProfile = userProfile,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { selectedProfile ->
                userProfile = selectedProfile
                user?.uid?.let { uid ->
                    db.collection("user_profiles").document(uid).set(
                        mapOf(
                            "name" to userProfile.name,
                            "dob" to userProfile.dob,
                            "qualification" to userProfile.qualification,
                            "avatarResourceId" to selectedProfile.avatarResourceId,
                            "avatarUri" to selectedProfile.avatarUri
                        )
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
                    db.collection("user_profiles").document(uid).set(
                        mapOf(
                            "name" to updatedProfile.name,
                            "dob" to updatedProfile.dob,
                            "qualification" to updatedProfile.qualification,
                            "avatarResourceId" to updatedProfile.avatarResourceId,
                            "avatarUri" to updatedProfile.avatarUri
                        )
                    )
                }
                showEditDialog = false
            }
        )
    }
}

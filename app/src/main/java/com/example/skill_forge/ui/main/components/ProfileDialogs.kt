package com.example.skill_forge.ui.main.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.skill_forge.R
import com.example.skill_forge.ui.main.models.UserProfile

@Composable
fun AvatarPickerDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onAvatarSelected: (UserProfile) -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Update profile with URI and clear the Resource ID
            onAvatarSelected(currentProfile.copy(avatarUri = it.toString(), avatarResourceId = null))
        }
    }

    val avatars = listOf(
        R.drawable.avatar_girl,
        R.drawable.avatar_boy,
        R.drawable.avatar_girl_1,
        R.drawable.avatar_user,
        R.drawable.avatar_man,
        R.drawable.avatar_display_pic,
        R.drawable.avatar_housekeeper,
        R.drawable.avatar_business_man,
        R.drawable.avatar_boy_1,
        R.drawable.avatar_woman
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2632)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            // REMOVED .verticalScroll here because LazyVerticalGrid handles its own scrolling.
            // Nested scrolling causes crashes.
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Choose Avatar",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Or select a preset avatar:",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Grid of Avatars
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f) // Takes available space
                ) {
                    items(avatars) { avatarRes ->
                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (currentProfile.avatarResourceId == avatarRes) Color(0xFF00E5FF) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    // Update profile with Resource ID and clear URI
                                    onAvatarSelected(
                                        currentProfile.copy(
                                            avatarResourceId = avatarRes,
                                            avatarUri = null
                                        )
                                    )
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    // Linked to UserProfile properties
    var name by remember { mutableStateOf(currentProfile.username) }
    var dob by remember { mutableStateOf(currentProfile.dob) }
    var qualification by remember { mutableStateOf(currentProfile.qualification) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2632)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Edit Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF00E5FF),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(
                                currentProfile.copy(
                                    username = name, // Map 'name' UI back to 'username' data
                                    dob = dob,
                                    qualification = qualification
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
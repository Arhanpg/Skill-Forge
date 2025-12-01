package com.example.skill_forge.ui.auth

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController
import com.example.skill_forge.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Composable
fun AuthScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Animation for glowing logo
    val infiniteTransition = rememberInfiniteTransition(label = "glowing")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(glowScale)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Skill Forge",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Master Your Skills Through Battle",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Cyan
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = authTextFieldColors(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = authTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message Card
            if (errorMsg.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x44FF5555)
                    )
                ) {
                    Text(
                        errorMsg,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email/Password Login/Register Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMsg = "Please enter email and password"
                        return@Button
                    }

                    loading = true
                    errorMsg = ""

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                loading = false
                                navigateToMain(navController)
                            } else {
                                // Try registration if login fails
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { regTask ->
                                        if (regTask.isSuccessful) {
                                            saveUserToFirestore(
                                                db, auth, username, email,
                                                onSuccess = {
                                                    loading = false
                                                    navigateToMain(navController)
                                                },
                                                onError = { error ->
                                                    errorMsg = error
                                                    loading = false
                                                }
                                            )
                                        } else {
                                            errorMsg = regTask.exception?.message ?: "Registration failed"
                                            loading = false
                                        }
                                    }
                            }
                        }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Login / Register",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.Gray, thickness = 1.dp)
                Text(
                    "  OR  ",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
                Divider(modifier = Modifier.weight(1f), color = Color.Gray, thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        errorMsg = ""
                        signInWithGoogle(
                            context = context,
                            auth = auth,
                            db = db,
                            onSuccess = {
                                loading = false
                                navigateToMain(navController)
                            },
                            onError = { error ->
                                errorMsg = error
                                loading = false
                            }
                        )
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Sign in with Google",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// FIXED: Google Sign-In with proper error handling and Web Client ID from resources
suspend fun signInWithGoogle(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val credentialManager = CredentialManager.create(context)

        // Generate nonce for security
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        // Get Web Client ID from google-services.json automatically
        val webClientId = try {
            context.getString(
                context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
            )
        } catch (e: Exception) {
            onError("Firebase configuration error. Please check google-services.json")
            return
        }

        // FIXED: Configuration for first-time and repeat sign-ins
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Allow new accounts
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false) // Let user choose account
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken

        // Sign in to Firebase with Google credential
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(
                            db = db,
                            auth = auth,
                            username = it.displayName ?: "User",
                            email = it.email ?: "",
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    } ?: onError("User data not found")
                } else {
                    onError("Firebase authentication failed: ${task.exception?.message}")
                }
            }
    } catch (e: GetCredentialException) {
        // Better error message for common issues
        when {
            e.message?.contains("No credentials available") == true -> {
                onError("Please ensure:\n1. Google account is added to device\n2. SHA-1 is configured in Firebase\n3. Google Sign-In is enabled in Firebase Console")
            }
            e.message?.contains("cancelled") == true -> {
                onError("Sign-in cancelled")
            }
            else -> {
                onError("Google Sign-In error: ${e.message}")
            }
        }
    } catch (e: Exception) {
        onError("Error: ${e.localizedMessage ?: e.message}")
    }
}

// Helper function to save user data to Firestore
fun saveUserToFirestore(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    username: String,
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val userId = auth.currentUser?.uid
    if (userId == null) {
        onError("User ID not found")
        return
    }

    // Check if user already exists
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (!document.exists()) {
                // Create new user document
                db.collection("users")
                    .document(userId)
                    .set(
                        mapOf(
                            "username" to username,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis(),
                            "avatarCustomization" to ""
                        )
                    )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError("Error saving profile: ${e.message}")
                    }
            } else {
                // User already exists, just proceed
                onSuccess()
            }
        }
        .addOnFailureListener { e ->
            onError("Database error: ${e.message}")
        }
}

// Helper function to navigate to main screen
fun navigateToMain(navController: NavHostController) {
    navController.navigate("main") {
        popUpTo("auth") { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun authTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color.Cyan,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color.Yellow,
        unfocusedLabelColor = Color.LightGray,
        cursorColor = Color.Cyan,
        unfocusedContainerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent
    )
}

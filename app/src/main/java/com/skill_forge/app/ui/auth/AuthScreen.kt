package com.skill_forge.app.ui.auth

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import com.skill_forge.app.R

// Define Auth Modes to toggle between Login and Signup
enum class AuthMode {
    LOGIN, SIGN_UP
}

@Composable
fun AuthScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    // FIX: Connect to the specific database "skillforge"
    val db = remember { FirebaseFirestore.getInstance("skillforge") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Scroll State
    val scrollState = rememberScrollState()

    // Colors
    val primaryCyan = Color(0xFF00BCD4)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    // Animation for zooming logo
    val infiniteTransition = rememberInfiniteTransition(label = "zooming")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f, // Subtle zoom
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // ENABLE SCROLLING
                .padding(24.dp)
                .padding(top = 16.dp)
                .imePadding(), // HANDLE KEYBOARD PADDING
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- LOGO SECTION ---
            Image(
                painter = painterResource(id = R.drawable.skillforge_logo1),
                contentDescription = "Skill Forge Logo",
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Master Your Skills Through Battle",
                style = MaterialTheme.typography.bodyMedium,
                color = primaryCyan,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- TABS (LOGIN / REGISTER) ---
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthTabButton(
                        text = "Log In",
                        selected = authMode == AuthMode.LOGIN,
                        onClick = { authMode = AuthMode.LOGIN },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTabButton(
                        text = "Sign Up",
                        selected = authMode == AuthMode.SIGN_UP,
                        onClick = { authMode = AuthMode.SIGN_UP },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- INPUT FIELDS ---
            AnimatedVisibility(
                visible = authMode == AuthMode.SIGN_UP,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    ModernAuthTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        icon = Icons.Default.Person,
                        primaryColor = primaryCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            ModernAuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                icon = Icons.Default.Email,
                primaryColor = primaryCyan,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModernAuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Default.Lock,
                primaryColor = primaryCyan,
                isPassword = true,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- ERROR MESSAGE ---
            if (errorMsg.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xAAFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- MAIN ACTION BUTTON ---
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in all fields"
                        return@Button
                    }
                    if (authMode == AuthMode.SIGN_UP && username.isBlank()) {
                        errorMsg = "Username is required for sign up"
                        return@Button
                    }

                    loading = true
                    errorMsg = ""

                    if (authMode == AuthMode.LOGIN) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    loading = false
                                    navigateToMain(navController)
                                } else {
                                    errorMsg = task.exception?.message ?: "Login failed"
                                    loading = false
                                }
                            }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
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
                                    errorMsg = task.exception?.message ?: "Registration failed"
                                    loading = false
                                }
                            }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    disabledContainerColor = primaryCyan.copy(alpha = 0.5f)
                ),
                shape = CircleShape
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "LOGIN" else "CREATE ACCOUNT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DIVIDER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
                Text(
                    text = "OR",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- GOOGLE SIGN IN ---
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.LightGray
                ),
                shape = CircleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Sign in with Google",
                        color = Color.Black.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// --- CUSTOM UI COMPONENTS ---

@Composable
fun AuthTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) Color(0xFF00BCD4) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    primaryColor: Color,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (value.isNotEmpty()) primaryColor else Color.Gray
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = Color.Gray,
            cursorColor = primaryColor
        ),
        singleLine = true
    )
}

// --- HELPER FUNCTIONS ---

suspend fun signInWithGoogle(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val credentialManager = CredentialManager.create(context)
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val webClientId = try {
            context.getString(
                context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
            )
        } catch (e: Exception) {
            onError("Firebase configuration error. Check google-services.json")
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(request = request, context = context)
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken
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
                    onError("Authentication failed: ${task.exception?.message}")
                }
            }
    } catch (e: GetCredentialException) {
        when {
            e.message?.contains("No credentials available") == true -> onError("Google Sign-In failed. Check configuration.")
            e.message?.contains("cancelled") == true -> onError("Sign-in cancelled")
            else -> onError("Google Sign-In error: ${e.message}")
        }
    } catch (e: Exception) {
        onError("Error: ${e.localizedMessage}")
    }
}

fun saveUserToFirestore(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    username: String,
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val userId = auth.currentUser?.uid ?: return onError("User ID not found")

    // FIX: Explicitly initialize XP, Coins, and Streak to avoid crashes on Home Screen
    val userData = hashMapOf(
        "username" to username,
        "email" to email,
        "createdAt" to System.currentTimeMillis(),
        "avatarCustomization" to "",
        "xp" to 0,
        "coins" to 0,
        "streakDays" to 0
    )

    // Use SetOptions.merge() to prevent overwriting existing data if the user logs in again
    db.collection("users").document(userId)
        .set(userData, SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onError("Error saving profile: ${e.message}") }
}

fun navigateToMain(navController: NavHostController) {
    navController.navigate("main") {
        popUpTo("auth") { inclusive = true }
        launchSingleTop = true
    }
}
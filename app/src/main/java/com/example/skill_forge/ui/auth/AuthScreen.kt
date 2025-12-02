package com.example.skill_forge.ui.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavHostController
import com.example.skill_forge.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "AuthScreen"
private const val TIMEOUT_DURATION = 10000L // 10 seconds timeout

enum class AuthMode {
    LOGIN, SIGN_UP
}

@Composable
fun AuthScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val primaryCyan = Color(0xFF00BCD4)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "zooming")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
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
                .verticalScroll(scrollState)
                .padding(24.dp)
                .padding(top = 16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                        onClick = {
                            authMode = AuthMode.LOGIN
                            errorMsg = "" // Clear error on tab switch
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTabButton(
                        text = "Sign Up",
                        selected = authMode == AuthMode.SIGN_UP,
                        onClick = {
                            authMode = AuthMode.SIGN_UP
                            errorMsg = "" // Clear error on tab switch
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Button(
                onClick = {
                    if (!isNetworkAvailable(context)) {
                        errorMsg = "No internet connection. Please check your network."
                        return@Button
                    }

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

                    scope.launch {
                        try {
                            if (authMode == AuthMode.LOGIN) {
                                // EMAIL LOGIN WITH TIMEOUT
                                val loginResult = withTimeoutOrNull(TIMEOUT_DURATION) {
                                    auth.signInWithEmailAndPassword(email, password).await()
                                }

                                if (loginResult != null) {
                                    Log.d(TAG, "Email login successful")
                                    loading = false
                                    // Force navigation
                                    navigateToMainScreen(navController)
                                } else {
                                    errorMsg = "Login timeout. Please try again."
                                    loading = false
                                }
                            } else {
                                // SIGN UP WITH TIMEOUT
                                val signUpResult = withTimeoutOrNull(TIMEOUT_DURATION) {
                                    auth.createUserWithEmailAndPassword(email, password).await()
                                }

                                if (signUpResult != null) {
                                    Log.d(TAG, "Sign up successful")
                                    // Save user data in background, don't block navigation
                                    saveUserToFirestoreAsync(db, auth, username, email)
                                    loading = false
                                    navigateToMainScreen(navController)
                                } else {
                                    errorMsg = "Sign up timeout. Please try again."
                                    loading = false
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Auth error", e)
                            errorMsg = e.message ?: "Authentication failed"
                            loading = false
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

            Button(
                onClick = {
                    if (!isNetworkAvailable(context)) {
                        errorMsg = "No internet connection. Please check your network."
                        return@Button
                    }

                    loading = true
                    errorMsg = ""

                    scope.launch {
                        try {
                            signInWithGoogleFixed(
                                context = context,
                                auth = auth,
                                db = db,
                                onSuccess = {
                                    loading = false
                                    navigateToMainScreen(navController)
                                },
                                onError = { error ->
                                    errorMsg = error
                                    loading = false
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Google Sign-In error", e)
                            errorMsg = "Google Sign-In failed: ${e.message}"
                            loading = false
                        }
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

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

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

// FIX: Samsung S23 Ultra compatible Google Sign-In
suspend fun signInWithGoogleFixed(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d(TAG, "Starting Google Sign-In (Samsung S23 Ultra Compatible)")

        // Check if Credential Manager is supported
        val credentialManager = try {
            CredentialManager.create(context)
        } catch (e: Exception) {
            Log.e(TAG, "Credential Manager not supported", e)
            onError("Your device doesn't support Google Sign-In via Credential Manager. Please use email login.")
            return
        }

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
            Log.e(TAG, "Failed to get web client ID", e)
            onError("Firebase configuration error. Please check google-services.json")
            return
        }

        Log.d(TAG, "Building Google ID Option")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // CRITICAL: Allow account picker
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false) // CRITICAL: Disable auto-select for Samsung devices
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        Log.d(TAG, "Requesting credentials with timeout...")

        // FIX: Add timeout for Samsung devices that hang
        val credentialResult = withTimeoutOrNull(15000L) { // 15 second timeout
            credentialManager.getCredential(request = request, context = context)
        }

        if (credentialResult == null) {
            Log.e(TAG, "Credential request timed out")
            onError("Google Sign-In timed out. Please try again or use email login.")
            return
        }

        val credential = credentialResult.credential

        Log.d(TAG, "Credentials obtained, creating token")
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

        Log.d(TAG, "Signing in to Firebase")

        // FIX: Add timeout for Firebase sign-in
        val authResult = withTimeoutOrNull(TIMEOUT_DURATION) {
            auth.signInWithCredential(firebaseCredential).await()
        }

        if (authResult == null) {
            onError("Firebase sign-in timed out. Please try again.")
            return
        }

        val user = auth.currentUser
        if (user != null) {
            Log.d(TAG, "User signed in: ${user.uid}")

            // Save user data in background, don't block success callback
            saveUserToFirestoreAsync(
                db = db,
                auth = auth,
                username = user.displayName ?: "User",
                email = user.email ?: ""
            )

            // Immediately call success - don't wait for Firestore
            delay(100) // Small delay to ensure Firebase state is updated
            onSuccess()
        } else {
            Log.e(TAG, "User is null after sign-in")
            onError("Sign-in succeeded but user data is unavailable")
        }

    } catch (e: NoCredentialException) {
        Log.e(TAG, "No credentials available", e)
        onError("No Google account found. Please add a Google account to your device.")
    } catch (e: GetCredentialCancellationException) {
        Log.e(TAG, "User cancelled sign-in", e)
        onError("Sign-in cancelled")
    } catch (e: GetCredentialException) {
        Log.e(TAG, "Credential exception", e)
        onError("Google Sign-In error: ${e.message}")
    } catch (e: Exception) {
        Log.e(TAG, "General exception", e)
        onError("Error: ${e.localizedMessage}")
    }
}

// FIX: Non-blocking Firestore save
fun saveUserToFirestoreAsync(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    username: String,
    email: String
) {
    val userId = auth.currentUser?.uid ?: return

    Log.d(TAG, "Saving user to Firestore (async): $userId")

    val userData = hashMapOf(
        "username" to username,
        "email" to email,
        "createdAt" to System.currentTimeMillis(),
        "avatarCustomization" to ""
    )

    // Use set with merge - don't wait for completion
    db.collection("users")
        .document(userId)
        .set(userData, com.google.firebase.firestore.SetOptions.merge())
        .addOnSuccessListener {
            Log.d(TAG, "User saved successfully")
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Failed to save user (non-critical)", e)
        }
}

// FIX: Guaranteed navigation with delay
fun navigateToMainScreen(navController: NavHostController) {
    try {
        Log.d(TAG, "Navigating to main screen")
        navController.navigate("main") {
            popUpTo("auth") { inclusive = true }
            launchSingleTop = true
        }
        Log.d(TAG, "Navigation command executed")
    } catch (e: Exception) {
        Log.e(TAG, "Navigation error", e)
    }
}

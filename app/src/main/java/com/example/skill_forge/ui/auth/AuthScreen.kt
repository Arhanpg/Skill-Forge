package com.example.skill_forge.ui.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
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
import com.example.skill_forge.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "AuthScreen"

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
                                    scope.launch {
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
                                    }
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

                    scope.launch {
                        loading = true
                        errorMsg = ""
                        try {
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
                        } catch (e: Exception) {
                            errorMsg = "Sign-in failed: ${e.message}"
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

suspend fun signInWithGoogle(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d(TAG, "Starting Google Sign-In")

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
            Log.e(TAG, "Failed to get web client ID", e)
            onError("Firebase configuration error. Check google-services.json")
            return
        }

        Log.d(TAG, "Web Client ID obtained")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        Log.d(TAG, "Getting credentials...")
        val result = credentialManager.getCredential(request = request, context = context)
        val credential = result.credential

        Log.d(TAG, "Credentials obtained, creating token")
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

        Log.d(TAG, "Signing in to Firebase")
        auth.signInWithCredential(firebaseCredential).await()

        val user = auth.currentUser
        if (user != null) {
            Log.d(TAG, "User signed in: ${user.uid}")
            // Just navigate - don't check Firestore yet
            onSuccess()
        } else {
            Log.e(TAG, "User is null after sign-in")
            onError("Sign-in succeeded but user data is unavailable")
        }

    } catch (e: GetCredentialException) {
        Log.e(TAG, "Credential exception", e)
        when {
            e.message?.contains("No credentials available") == true ->
                onError("No Google account found. Please add a Google account to your device.")
            e.message?.contains("cancelled") == true ->
                onError("Sign-in cancelled")
            else ->
                onError("Google Sign-In error: ${e.message}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "General exception", e)
        onError("Error: ${e.localizedMessage}")
    }
}

// SIMPLIFIED: Just create user, don't check if exists
suspend fun saveUserToFirestore(
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

    try {
        Log.d(TAG, "Saving user to Firestore: $userId")

        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "createdAt" to System.currentTimeMillis(),
            "avatarCustomization" to ""
        )

        // Just set the data, use merge to avoid overwriting existing data
        db.collection("users")
            .document(userId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .await()

        Log.d(TAG, "User saved successfully")
        onSuccess()

    } catch (e: Exception) {
        Log.e(TAG, "Error saving user", e)
        // Even if Firestore fails, let user proceed
        Log.w(TAG, "Firestore save failed but allowing user to proceed")
        onSuccess() // Proceed anyway
    }
}

fun navigateToMain(navController: NavHostController) {
    navController.navigate("main") {
        popUpTo("auth") { inclusive = true }
        launchSingleTop = true
    }
}

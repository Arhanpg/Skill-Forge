package com.example.skill_forge.ui.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.*
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "AuthScreen"

// DEFINED ENUM HERE TO FIX UNRESOLVED REFERENCE
enum class AuthMode {
    LOGIN, SIGN_UP
}

@Composable
fun AuthScreen(navController: NavHostController) {
    // FIX: Connect explicitly to "skillforge" database
    val db = remember { FirebaseFirestore.getInstance("skillforge") }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // FIX: Explicitly typed to prevent inference error
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "yOffset"
    )

    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        startY = yOffset
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "scale"
            )

            Image(
                painter = painterResource(id = R.drawable.skillforge_logo1),
                contentDescription = "Logo",
                modifier = Modifier.size(180.dp).scale(scale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "SKILL FORGE",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.3f), CircleShape)
                            .padding(4.dp)
                    ) {
                        AuthTab(
                            text = "Login",
                            selected = authMode == AuthMode.LOGIN,
                            onClick = { authMode = AuthMode.LOGIN; errorMsg = null }
                        )
                        AuthTab(
                            text = "Sign Up",
                            selected = authMode == AuthMode.SIGN_UP,
                            onClick = { authMode = AuthMode.SIGN_UP; errorMsg = null }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedVisibility(visible = authMode == AuthMode.SIGN_UP) {
                        Column {
                            AuthTextField(value = username, onValueChange = { username = it }, label = "Username", icon = Icons.Default.Person)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    AuthTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email, keyboardType = KeyboardType.Email)
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (!isNetworkAvailable(context)) {
                                errorMsg = "No internet connection."
                                return@Button
                            }
                            loading = true
                            scope.launch {
                                try {
                                    if (authMode == AuthMode.LOGIN) {
                                        auth.signInWithEmailAndPassword(email, password).await()
                                    } else {
                                        val res = auth.createUserWithEmailAndPassword(email, password).await()
                                        res.user?.let {
                                            saveUserToFirestore(db, it.uid, username, email)
                                        }
                                    }
                                    navigateToMainScreen(navController)
                                } catch (e: Exception) {
                                    errorMsg = e.message
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text(if (authMode == AuthMode.LOGIN) "ENTER REALM" else "CREATE ACCOUNT", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            loading = true
                            scope.launch {
                                signInWithGoogle(context, auth, db,
                                    onSuccess = { navigateToMainScreen(navController) },
                                    onError = { errorMsg = it; loading = false }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray) // This was the unresolved reference
                    ) {
                        Image(painter = painterResource(id = R.drawable.google_logo), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continue with Google")
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.AuthTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF00BCD4) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Text(text, color = if (selected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF00BCD4)) },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) { { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if(passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00BCD4),
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

fun saveUserToFirestore(db: FirebaseFirestore, userId: String, username: String, email: String) {
    val userMap = hashMapOf("username" to username, "email" to email, "createdAt" to System.currentTimeMillis())
    db.collection("users").document(userId).set(userMap, SetOptions.merge())
}

suspend fun signInWithGoogle(context: Context, auth: FirebaseAuth, db: FirebaseFirestore, onSuccess: () -> Unit, onError: (String) -> Unit) {
    try {
        val credentialManager = CredentialManager.create(context)
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val webClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        val authResult = auth.signInWithCredential(firebaseCredential).await()
        if (authResult.user != null) {
            saveUserToFirestore(db, authResult.user!!.uid, authResult.user!!.displayName ?: "Hero", authResult.user!!.email ?: "")
            onSuccess()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Google Sign In Failed", e)
        if (e is GetCredentialException) onError("Sign in cancelled") else onError(e.message ?: "Login Failed")
    }
}

fun navigateToMainScreen(navController: NavHostController) {
    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
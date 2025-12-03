package com.skill_forge.app.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers // Required for crash fix
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Required for crash fix
import org.json.JSONObject

@Composable
fun QuizScreen() {
    val scope = rememberCoroutineScope()

    // UI State
    var topic by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentQuestion by remember { mutableStateOf<QuizData?>(null) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var resultMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2027)) // Dark background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI KNOWLEDGE FORGE",
            color = Color(0xFF00E5FF),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // Input Area
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Enter Topic (e.g. Kotlin Coroutines)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === GENERATE BUTTON ===
        Button(
            onClick = {
                if (topic.isNotBlank()) {
                    scope.launch {
                        isLoading = true
                        currentQuestion = null // Reset previous question
                        selectedOption = null
                        resultMessage = ""

                        // FIX: Run network call on IO thread to prevent crash
                        val jsonString = withContext(Dispatchers.IO) {
                            try {
                                // This calls the file GeminiService.kt we created
                                GeminiService.generateQuizQuestion(topic)
                            } catch (e: Exception) {
                                // This prevents the crash if something fails
                                e.printStackTrace()
                                "{}" // Return empty JSON on error
                            }
                        }

                        // UI updates must happen back on Main thread (automatic in Compose)
                        if (jsonString != "{}") {
                            currentQuestion = parseQuizJson(jsonString)
                        }
                        isLoading = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9)), // Epic Purple
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forging Question...")
            } else {
                Text("Generate Quiz")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // === QUIZ DISPLAY AREA ===
        currentQuestion?.let { q ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = q.question, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(16.dp))

                    q.options.forEachIndexed { index, optionText ->
                        val isSelected = selectedOption == index
                        val isCorrect = index == q.correctIndex

                        // Color Logic
                        val borderColor = when {
                            selectedOption != null && isCorrect -> Color.Green
                            selectedOption == index && !isCorrect -> Color.Red
                            isSelected -> Color(0xFF00E5FF)
                            else -> Color.Gray
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .background(if (isSelected) borderColor.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable(enabled = selectedOption == null) {
                                    selectedOption = index
                                    resultMessage = if (index == q.correctIndex) "Correct! +50 XP" else "Incorrect."
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = optionText, color = Color.White)
                        }
                    }

                    if (resultMessage.isNotEmpty()) {
                        Text(
                            text = resultMessage,
                            color = if (resultMessage.contains("Correct")) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

// === DATA MODELS & HELPERS ===

data class QuizData(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

// In QuizScreen.kt (at the bottom)

// In QuizScreen.kt

fun parseQuizJson(jsonResponse: String): QuizData? {
    try {
        // 1. Sanitize the input: Remove Markdown code blocks if the AI disobeyed
        var cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim()

        // 2. Find the JSON object brackets explicitly
        val startIndex = cleanJson.indexOf('{')
        val endIndex = cleanJson.lastIndexOf('}')

        if (startIndex != -1 && endIndex != -1) {
            cleanJson = cleanJson.substring(startIndex, endIndex + 1)
        } else {
            // No brackets found means the AI returned a refusal or error text
            Log.e("QuizParser", "Invalid JSON format received: $jsonResponse")
            return QuizData("AI Error: Could not generate question.", listOf("Try Again", "-", "-", "-"), 0)
        }

        // 3. Parse JSON
        val jsonObject = JSONObject(cleanJson)
        val question = jsonObject.getString("question")
        val correctIndex = jsonObject.getInt("correctIndex")
        val optionsArray = jsonObject.getJSONArray("options")

        val options = mutableListOf<String>()
        for (i in 0 until optionsArray.length()) {
            options.add(optionsArray.getString(i))
        }

        return QuizData(question, options, correctIndex)

    } catch (e: Exception) {
        // 4. Catch parsing errors (e.g. missing keys)
        Log.e("QuizParser", "Parsing failed", e)
        return QuizData("Error parsing quiz data.", listOf("Retry", "-", "-", "-"), 0)
    }
}

package com.skill_forge.app.ui.main.screens

import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun QuizScreen() {
    val scope = rememberCoroutineScope()

    // === UI STATE ===
    var topic by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Quiz Game State
    var quizQuestions by remember { mutableStateOf<List<QuizData>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var resultMessage by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

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

        // === 1. INPUT PHASE (Show only if no questions generated) ===
        if (quizQuestions.isEmpty() && !isLoading) {
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

            Button(
                onClick = {
                    if (topic.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            isQuizFinished = false
                            score = 0
                            currentQuestionIndex = 0
                            selectedOption = null
                            resultMessage = ""

                            // Run network call on IO thread
                            val jsonString = withContext(Dispatchers.IO) {
                                try {
                                    GeminiService.generateQuizQuestion(topic)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    "[]" // Return empty array string on error
                                }
                            }

                            // Parse the list of questions
                            val parsedQuestions = parseQuizJson(jsonString)

                            if (parsedQuestions.isNotEmpty()) {
                                quizQuestions = parsedQuestions
                            } else {
                                resultMessage = "Failed to generate questions. Try again."
                            }
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Quiz")
            }

            if (resultMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(resultMessage, color = Color.Red)
            }
        }

        // === 2. LOADING PHASE ===
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Forging Questions...", color = Color.White)
                }
            }
        }

        // === 3. QUIZ ACTIVE PHASE ===
        if (quizQuestions.isNotEmpty() && !isQuizFinished) {
            val currentQ = quizQuestions[currentQuestionIndex]

            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1) / quizQuestions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF00E5FF),
                trackColor = Color.DarkGray,
            )
            Text(
                text = "Question ${currentQuestionIndex + 1} of ${quizQuestions.size}",
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentQ.question,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    currentQ.options.forEachIndexed { index, optionText ->
                        val isSelected = selectedOption == index
                        val isCorrect = index == currentQ.correctIndex
                        val isAnswerRevealed = selectedOption != null

                        // Color Logic
                        val borderColor = when {
                            isAnswerRevealed && index == currentQ.correctIndex -> Color.Green // Show correct always
                            isAnswerRevealed && isSelected && !isCorrect -> Color.Red // Show wrong if selected
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
                                    if (index == currentQ.correctIndex) {
                                        score += 10
                                        resultMessage = "Correct! +10 XP"
                                    } else {
                                        resultMessage = "Wrong! The answer was option ${currentQ.correctIndex + 1}"
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = optionText, color = Color.White)
                        }
                    }
                }
            }

            // Result & Next Button
            if (selectedOption != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = resultMessage,
                    color = if (resultMessage.contains("Correct")) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (currentQuestionIndex < quizQuestions.size - 1) {
                            // Move to next question
                            currentQuestionIndex++
                            selectedOption = null
                            resultMessage = ""
                        } else {
                            // End Quiz
                            isQuizFinished = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentQuestionIndex < quizQuestions.size - 1) "Next Question" else "Finish Quiz")
                }
            }
        }

        // === 4. QUIZ FINISHED PHASE ===
        if (isQuizFinished) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("QUIZ COMPLETED!", color = Color(0xFFD500F9), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your Score", color = Color.Gray, fontSize = 16.sp)
                    Text("$score / ${quizQuestions.size * 10}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // Reset everything
                            quizQuestions = emptyList()
                            topic = ""
                            isLoading = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Forge New Quiz")
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

/**
 * Parses the JSON string from Gemini.
 * Handles both single JSON Objects OR JSON Arrays (List of questions).
 */
fun parseQuizJson(jsonResponse: String): List<QuizData> {
    val questionsList = mutableListOf<QuizData>()

    try {
        // 1. Sanitize: Remove Markdown code blocks
        var cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim()

        // 2. Determine if it is an Array [...] or Object {...}
        if (cleanJson.startsWith("[")) {
            // It's a JSON Array (Multiple Questions)
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                questionsList.add(parseSingleJsonObj(obj))
            }
        } else if (cleanJson.startsWith("{")) {
            // It's a single JSON Object
            val jsonObject = JSONObject(cleanJson)
            questionsList.add(parseSingleJsonObj(jsonObject))
        } else {
            Log.e("QuizParser", "Invalid JSON format: $cleanJson")
        }

    } catch (e: Exception) {
        Log.e("QuizParser", "Parsing failed", e)
    }

    return questionsList
}

// Helper to avoid duplicate code
fun parseSingleJsonObj(jsonObject: JSONObject): QuizData {
    val question = jsonObject.getString("question")
    val correctIndex = jsonObject.getInt("correctIndex")
    val optionsArray = jsonObject.getJSONArray("options")

    val options = mutableListOf<String>()
    for (i in 0 until optionsArray.length()) {
        options.add(optionsArray.getString(i))
    }
    return QuizData(question, options, correctIndex)
}
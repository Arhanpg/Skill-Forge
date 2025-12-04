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
import androidx.compose.ui.unit.sp
import com.skill_forge.app.network.GeminiService
// IMPORT SHARED UTILS AND MODELS
import com.skill_forge.app.ui.main.models.QuizQuestion
import com.skill_forge.app.utils.QuizUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuizScreen() {
    val scope = rememberCoroutineScope()

    // === UI STATE ===
    var topic by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Quiz Game State - Uses QuizQuestion from QuizModels.kt
    var quizQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var resultMessage by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2027))
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

        // === 1. INPUT PHASE ===
        if (quizQuestions.isEmpty() && !isLoading) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Enter Topic") },
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

                            val jsonString = withContext(Dispatchers.IO) {
                                try {
                                    GeminiService.generateQuizQuestion(topic)
                                } catch (e: Exception) {
                                    "[]"
                                }
                            }

                            // USE SHARED PARSER HERE
                            val parsedQuestions = QuizUtils.parseQuizJson(jsonString)

                            if (parsedQuestions.isNotEmpty()) {
                                quizQuestions = parsedQuestions
                            } else {
                                resultMessage = "Failed to generate. Try again."
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
                CircularProgressIndicator(color = Color(0xFF00E5FF))
            }
        }

        // === 3. QUIZ ACTIVE PHASE ===
        if (quizQuestions.isNotEmpty() && !isQuizFinished) {
            val currentQ = quizQuestions[currentQuestionIndex]

            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1) / quizQuestions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF00E5FF),
                trackColor = Color.DarkGray,
            )
            Text("Question ${currentQuestionIndex + 1} / ${quizQuestions.size}", color = Color.Gray)

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)), modifier = Modifier.fillMaxWidth().padding(top=16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentQ.question, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))

                    currentQ.options.forEachIndexed { index, optionText ->
                        val isSelected = selectedOption == index
                        val isCorrect = index == currentQ.correctIndex
                        val isRevealed = selectedOption != null

                        val borderColor = when {
                            isRevealed && index == currentQ.correctIndex -> Color.Green
                            isRevealed && isSelected && !isCorrect -> Color.Red
                            isSelected -> Color(0xFF00E5FF)
                            else -> Color.Gray
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .background(if (isSelected) borderColor.copy(0.1f) else Color.Transparent)
                                .clickable(enabled = selectedOption == null) {
                                    selectedOption = index
                                    if (index == currentQ.correctIndex) {
                                        score += 10
                                        resultMessage = "Correct! +10 XP"
                                    } else {
                                        resultMessage = "Wrong!"
                                    }
                                }.padding(16.dp)
                        ) {
                            Text(text = optionText, color = Color.White)
                        }
                    }
                }
            }

            if (selectedOption != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(resultMessage, color = if(resultMessage.contains("Correct")) Color.Green else Color.Red, fontSize=18.sp)
                Button(
                    onClick = {
                        if (currentQuestionIndex < quizQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedOption = null
                            resultMessage = ""
                        } else {
                            isQuizFinished = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(if (currentQuestionIndex < quizQuestions.size - 1) "Next" else "Finish")
                }
            }
        }

        // === 4. FINISHED ===
        if (isQuizFinished) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMPLETED!", color = Color(0xFFD500F9), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Score: $score", color = Color.White, fontSize = 32.sp)
                    Button(onClick = { quizQuestions = emptyList(); topic = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("New Quiz")
                    }
                }
            }
        }
    }
}

/*

Copyright 2025 A^3*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at*
http://www.apache.org/licenses/LICENSE-2.0*
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/


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
package com.skill_forge.app.network

import android.util.Log
import com.google.gson.Gson
import com.skill_forge.app.ui.main.models.QuizQuestion
import com.skill_forge.app.ui.main.models.ServerRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit // Required for setting time duration

// ==================== API INTERFACE ====================
interface QuizApi {
    @POST("generate-quiz")
    suspend fun getQuiz(@Body request: ServerRequest): List<QuizQuestion>
}

// ==================== SERVICE ====================
object GeminiService {

    // IMPORTANT: Update this NGROK URL every time you restart your Python Colab script
    private const val BASE_URL = ""//update this with the url obtained

    // FIX: Custom HTTP Client with Extended Timeouts
    // This prevents the app from crashing while waiting for the AI to generate answers.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Wait 60s to find server
            .readTimeout(60, TimeUnit.SECONDS)    // Wait 60s for AI response (Crucial)
            .writeTimeout(60, TimeUnit.SECONDS)   // Wait 60s to send data
            .build()
    }

    private val api: QuizApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // <--- ATTACH THE CUSTOM CLIENT HERE
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuizApi::class.java)
    }

    // Used by HomeViewModel (Returns JSON String)
    suspend fun generateQuizQuestion(topic: String): String {
        return try {
            Log.d("GeminiService", "Requesting quiz for: $topic")

            // 1. Network Call -> Returns List<QuizQuestion> directly
            val responseList = api.getQuiz(ServerRequest(topic))

            Log.d("GeminiService", "Received ${responseList.size} questions")

            // 2. Convert to JSON String to pass to ViewModel
            Gson().toJson(responseList)

        } catch (e: Exception) {
            Log.e("GeminiService", "Server Error", e)
            // Return Error JSON so the app doesn't crash, but shows a message
            """
            [
                {
                    "question": "Connection Failed: ${e.message}",
                    "options": ["Check URL", "Check Internet", "Retry", "Skip"],
                    "correctIndex": 0
                }
            ]
            """.trimIndent()
        }
    }

    // Used by Task/Quest Logic (Returns List Object directly)
    suspend fun generateTaskQuiz(topic: String): List<QuizQuestion> {
        return try {
            Log.d("GeminiService", "Requesting quiz (List) for: $topic")
            api.getQuiz(ServerRequest(topic)) // Returns List directly
        } catch (e: Exception) {
            Log.e("GeminiService", "Server Error", e)
            listOf(
                QuizQuestion(
                    question = "Connection Failed: ${e.message}",
                    options = listOf("Check URL", "Check Internet", "Retry", "Skip"),
                    correctIndex = 0
                )
            )
        }
    }

    suspend fun generateQuestLoot(questTitle: String): String {
        // Simulating AI Loot generation for now
        kotlinx.coroutines.delay(1000)
        return """
            // AI REWARD FOR: $questTitle
            val loot = "Rare Gem"
            val bonusXp = 500
            println("You found a hidden item!")
        """.trimIndent()
    }
}


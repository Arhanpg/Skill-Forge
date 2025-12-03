package com.skill_forge.app.ui.main.screens

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.Gson

// ==================== DATA MODELS ====================
data class ServerRequest(val topic: String)

data class QuizResponse(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

// ==================== API INTERFACE ====================
interface QuizApi {
    @POST("generate-quiz")
    suspend fun getQuiz(@Body request: ServerRequest): List<QuizResponse>
}

// ==================== SERVICE ====================
object GeminiService {

    // 🔴 TODO: PASTE YOUR NEW NGROK URL HERE (Must end with /)
    // Example: "https://a1b2-34-56.ngrok-free.app/"
    private const val BASE_URL = "https://ian-unmumbling-isobel.ngrok-free.dev/"

    private val api: QuizApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuizApi::class.java)
    }

    suspend fun generateQuizQuestion(topic: String): String {
        return try {
            Log.d("GeminiService", "Requesting quiz for: $topic")

            // 1. Network Call
            val responseList = api.getQuiz(ServerRequest(topic))

            // 2. Success Check
            Log.d("GeminiService", "Received ${responseList.size} questions")

            // 3. Convert List Object back to JSON String (if your UI expects a String)
            Gson().toJson(responseList)

        } catch (e: Exception) {
            Log.e("GeminiService", "Server Error", e)
            // Return valid JSON error structure so the UI shows the error cleanly
            """
            [
                {
                    "question": "Connection Failed: ${e.message}",
                    "options": ["Check URL in Code", "Check Colab", "Check Internet", "Retry"],
                    "correctIndex": 0
                }
            ]
            """.trimIndent()
        }
    }

    suspend fun generateQuestLoot(questTitle: String): String {
        return """
            // Loot for: $questTitle
            fun reward() {
                println("You completed $questTitle!")
            }
        """.trimIndent()
    }
}
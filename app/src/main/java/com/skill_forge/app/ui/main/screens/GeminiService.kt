package com.skill_forge.app.ui.main.screens

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.Gson

// ==================== DATA MODELS ====================
data class ServerRequest(val topic: String)

// Now MUST match Python → List of objects
data class QuizResponse(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

// ==================== API INTERFACE ====================
interface QuizApi {

    // Expecting a LIST (array)
    @POST("generate-quiz")
    suspend fun getQuiz(@Body request: ServerRequest): List<QuizResponse>
}

// ==================== SERVICE ====================
object GeminiService {

    private const val BASE_URL = "https://ian-unmumbling-isobel.ngrok-free.dev"

    private val api: QuizApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuizApi::class.java)
    }

    suspend fun generateQuizQuestion(topic: String): String {
        return try {
            val responseList = api.getQuiz(ServerRequest(topic))

            // Convert entire list → JSON string
            Gson().toJson(responseList)

        } catch (e: Exception) {
            Log.e("GeminiService", "Server Error", e)
            """
            [
                {
                    "question": "Error connecting to Server. Is Colab running?",
                    "options": ["Check URL", "Check Colab", "Check Wifi", "Retry"],
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

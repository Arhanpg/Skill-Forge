package com.skill_forge.app.network // Update package if yours is different

import android.util.Log
import com.google.gson.Gson
import com.skill_forge.app.ui.main.models.QuizQuestion
import com.skill_forge.app.ui.main.models.ServerRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// ==================== API INTERFACE ====================
interface QuizApi {
    @POST("generate-quiz")
    suspend fun getQuiz(@Body request: ServerRequest): List<QuizQuestion>
}

// ==================== SERVICE ====================
object GeminiService {

    // IMPORTANT: Update this NGROK URL every time you restart Colab
    private const val BASE_URL = "https://your-ngrok-url-here.ngrok-free.app/"

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

            // 1. Network Call -> Returns List<QuizQuestion> directly
            val responseList = api.getQuiz(ServerRequest(topic))

            Log.d("GeminiService", "Received ${responseList.size} questions")

            // 2. Convert to JSON String to pass to ViewModel (or return List directly if you prefer)
            // Using Gson to turn the list back into a String string for consistency with your parsing logic
            Gson().toJson(responseList)

        } catch (e: Exception) {
            Log.e("GeminiService", "Server Error", e)
            // Return Error JSON
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
        // Simulating AI Loot generation for now (since we don't have a specific endpoint)
        kotlinx.coroutines.delay(1000)
        return """
            // AI REWARD FOR: $questTitle
            val loot = "Rare Gem"
            val bonusXp = 500
            println("You found a hidden item!")
        """.trimIndent()
    }
}
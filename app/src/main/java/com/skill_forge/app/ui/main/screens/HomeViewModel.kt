package com.skill_forge.app.ui.main.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.skill_forge.app.network.GeminiService
import com.skill_forge.app.ui.main.models.* import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    // IMPORTANT: Matching the database instance used in your AuthScreen ("skillforge")
    private val db = FirebaseFirestore.getInstance("skillforge")
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: ""

    // --- STATE VARIABLES ---
    val userProfile = mutableStateOf<UserProfile?>(null)

    // Using SessionState from QuizModels.kt
    val state = mutableStateOf(SessionState.IDLE)

    val activeQuests = mutableStateOf<List<Quest>>(emptyList())

    // --- SELECTION & TIMER STATE ---
    val selectedSubQuestIds = mutableStateOf<Set<String>>(emptySet())
    val completedSubQuestIds = mutableStateOf<Set<String>>(emptySet())

    // Timer Defaults (25 mins)
    val selectedDurationMin = mutableFloatStateOf(25f)
    val totalTimeSeconds = mutableLongStateOf(25 * 60L)
    val timeRemaining = mutableLongStateOf(25 * 60L)
    val focusSeconds = mutableLongStateOf(0L)
    val distractionSeconds = mutableLongStateOf(0L)
    val pauseAllowanceSeconds = mutableLongStateOf(0L)

    // --- INTERNAL HELPERS ---
    private var timerJob: Job? = null
    private var pauseJob: Job? = null
    private var lastBackgroundTimestamp = 0L

    // Listeners (to stop memory leaks)
    private var profileListener: ListenerRegistration? = null
    private var questListener: ListenerRegistration? = null

    // --- QUIZ & REWARD STATE ---
    val sessionSummary = mutableStateOf("")
    val isGeneratingQuiz = mutableStateOf(false)
    val generatedQuiz = mutableStateOf<List<QuizQuestion>>(emptyList())
    val currentXpReward = mutableIntStateOf(0)
    val currentCoinReward = mutableIntStateOf(0)

    // ==================== INITIALIZATION ====================

    fun startListening() {
        if (userId.isEmpty()) return

        // 1. Listen to User Profile (Real-time XP/Coin updates)
        profileListener?.remove()
        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("HomeViewModel", "Profile listen error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val rawProfile = snapshot.toObject(UserProfile::class.java)
                    // Ensure we capture specific fields correctly even if auto-mapping fails
                    val xp = snapshot.getLong("xp")?.toInt() ?: 0
                    val coins = snapshot.getLong("coins")?.toInt() ?: 0
                    val streak = snapshot.getLong("streakDays")?.toInt() ?: 0

                    userProfile.value = rawProfile?.copy(xp = xp, coins = coins, streakDays = streak)
                }
            }

        // 2. Listen to Active Quests
        questListener?.remove()
        questListener = db.collection("users").document(userId).collection("quests")
            .whereEqualTo("status", 0) // Only get active quests
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    activeQuests.value = snapshot.toObjects(Quest::class.java)
                }
            }
    }

    // ==================== SESSION LOGIC ====================

    fun startSession(context: Context) {
        if (selectedSubQuestIds.value.isEmpty()) {
            Toast.makeText(context, "Select at least one sub-task!", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup Timer logic
        val duration = (selectedDurationMin.value.toInt() * 60).toLong()
        totalTimeSeconds.longValue = duration
        timeRemaining.longValue = duration
        pauseAllowanceSeconds.longValue = duration / 12 // 5 mins pause for 60 mins work
        focusSeconds.longValue = 0
        distractionSeconds.longValue = 0

        state.value = SessionState.RUNNING
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        pauseJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeRemaining.longValue > 0 && state.value == SessionState.RUNNING) {
                delay(1000)
                timeRemaining.longValue--
                focusSeconds.longValue++
            }
            if (timeRemaining.longValue <= 0) {
                state.value = SessionState.COMPLETION_SELECT
            }
        }
    }

    fun pauseSession() {
        state.value = SessionState.PAUSED
        timerJob?.cancel()
        pauseJob = viewModelScope.launch {
            while(pauseAllowanceSeconds.longValue > 0 && state.value == SessionState.PAUSED) {
                delay(1000)
                pauseAllowanceSeconds.longValue--
            }
            // Auto resume if pause allowance runs out
            if (pauseAllowanceSeconds.longValue <= 0 && state.value == SessionState.PAUSED) {
                resumeSession()
            }
        }
    }

    fun resumeSession() {
        state.value = SessionState.RUNNING
        startTimer()
    }

    fun abandonSession() {
        state.value = SessionState.IDLE
        timerJob?.cancel()
        pauseJob?.cancel()
        selectedSubQuestIds.value = emptySet()
    }

    // ==================== TASK MANAGEMENT ====================

    fun toggleSubTaskSelection(subId: String) {
        val current = selectedSubQuestIds.value.toMutableSet()
        if (current.contains(subId)) current.remove(subId) else current.add(subId)
        selectedSubQuestIds.value = current
    }

    fun toggleSubTaskCompletion(subId: String) {
        val current = completedSubQuestIds.value.toMutableSet()
        if (current.contains(subId)) current.remove(subId) else current.add(subId)
        completedSubQuestIds.value = current
    }

    fun confirmCompletion() {
        state.value = SessionState.REPORTING
    }

    // ==================== LIFECYCLE / DISTRACTION ====================

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (state.value == SessionState.RUNNING && lastBackgroundTimestamp != 0L) {
            val now = System.currentTimeMillis()
            val diffSeconds = (now - lastBackgroundTimestamp) / 1000

            if (diffSeconds > 0) {
                distractionSeconds.longValue += diffSeconds
                // Subtract distraction time from remaining time (penalty)
                timeRemaining.longValue = (timeRemaining.longValue - diffSeconds).coerceAtLeast(0)
            }

            lastBackgroundTimestamp = 0L

            if (timeRemaining.longValue <= 0) {
                state.value = SessionState.COMPLETION_SELECT
                timerJob?.cancel()
            }
        }
    }

    // ==================== AI & REWARDS ====================

    fun generateQuiz(context: Context) {
        isGeneratingQuiz.value = true
        viewModelScope.launch {
            // 1. Call the Service
            val jsonString = GeminiService.generateQuizQuestion(sessionSummary.value)

            // 2. Parse the result using helper
            val quiz = parseQuizJson(jsonString)

            if (quiz.isNotEmpty()) {
                generatedQuiz.value = quiz
                state.value = SessionState.QUIZ
            } else {
                Toast.makeText(context, "AI Failed to generate quiz. Try adding more details.", Toast.LENGTH_SHORT).show()
            }
            isGeneratingQuiz.value = false
        }
    }

    fun completeQuiz(score: Int) {
        // 1. Calculate Rewards based on formula
        val minutes = focusSeconds.longValue / 60.0

        // XP: 0.95 per minute + 1 per correct answer
        val xpCalc = (0.95 * minutes) + (1.0 * score)

        // Coins: 1 per minute + 1.5 per correct answer
        val coinCalc = (1.0 * minutes) + (1.5 * score)

        val newXpEarned = xpCalc.roundToInt()
        val newCoinsEarned = coinCalc.roundToInt()

        currentXpReward.intValue = newXpEarned
        currentCoinReward.intValue = newCoinsEarned

        // 2. Update Database
        val currentTotalXp = userProfile.value?.xp ?: 0
        val currentTotalCoins = userProfile.value?.coins ?: 0

        viewModelScope.launch(Dispatchers.IO) {
            val batch = db.batch()

            // A. Update Quest Completion Status
            activeQuests.value.forEach { quest ->
                // Check if this quest has any subtasks that were marked as done
                val relevantSubs = quest.subQuests.filter { completedSubQuestIds.value.contains(it.id) }

                if (relevantSubs.isNotEmpty()) {
                    val updatedSubQuests = quest.subQuests.map {
                        if (completedSubQuestIds.value.contains(it.id)) it.copy(isCompleted = true) else it
                    }

                    // Check if entire quest is now done
                    val isQuestDone = updatedSubQuests.all { it.isCompleted }

                    val ref = db.collection("users").document(userId).collection("quests").document(quest.id)
                    batch.update(ref, "subQuests", updatedSubQuests)

                    if (isQuestDone) {
                        batch.update(ref, "status", 1) // Mark as completed
                    }
                }
            }

            // B. Update User Stats (XP & Coins)
            val userRef = db.collection("users").document(userId)
            batch.update(userRef, mapOf(
                "xp" to (currentTotalXp + newXpEarned),
                "coins" to (currentTotalCoins + newCoinsEarned)
            ))

            try {
                batch.commit().await()
                Log.d("HomeViewModel", "Rewards Saved Successfully")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving rewards", e)
            }
        }

        state.value = SessionState.REWARD
    }

    fun resetSession() {
        state.value = SessionState.IDLE
        focusSeconds.longValue = 0
        distractionSeconds.longValue = 0
        selectedSubQuestIds.value = emptySet()
        completedSubQuestIds.value = emptySet()
        sessionSummary.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        questListener?.remove()
    }
}

// ==================== HELPER FUNCTIONS (Outside Class) ====================

fun parseQuizJson(jsonResponse: String): List<QuizQuestion> {
    val questionsList = mutableListOf<QuizQuestion>()
    try {
        // Sanitize string (remove markdown code blocks)
        var cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim()

        if (cleanJson.startsWith("[")) {
            // It's an Array of Questions
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                questionsList.add(parseObj(obj))
            }
        } else if (cleanJson.startsWith("{")) {
            // It's a Single Question Object
            questionsList.add(parseObj(JSONObject(cleanJson)))
        }
    } catch (e: Exception) {
        Log.e("QuizParser", "Parsing failed", e)
    }
    return questionsList
}

fun parseObj(json: JSONObject): QuizQuestion {
    val optionsArray = json.getJSONArray("options")
    val options = mutableListOf<String>()
    for (i in 0 until optionsArray.length()) {
        options.add(optionsArray.getString(i))
    }
    return QuizQuestion(
        question = json.getString("question"),
        options = options,
        correctIndex = json.getInt("correctIndex")
    )
}
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
import com.skill_forge.app.ui.main.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import com.skill_forge.app.R

// --- RANK DATA STRUCTURE ---
data class RankDefinition(
    val title: String,
    val xpRequired: Int,
    val drawableId: Int
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("skillforge")
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: ""

    // --- STATE VARIABLES ---
    val userProfile = mutableStateOf<UserProfile?>(null)
    val state = mutableStateOf(SessionState.IDLE)
    val activeQuests = mutableStateOf<List<Quest>>(emptyList())

    // --- SELECTION & TIMER STATE ---
    val selectedSubQuestIds = mutableStateOf<Set<String>>(emptySet())
    val completedSubQuestIds = mutableStateOf<Set<String>>(emptySet())
    val selectedDurationMin = mutableFloatStateOf(25f)

    // TIMER FIX: Using Absolute Timestamps instead of simple countdown
    private var sessionEndTimeMillis = 0L
    private var pauseRemainingMillis = 0L

    val timeRemaining = mutableLongStateOf(25 * 60L) // Display value in seconds
    val totalTimeSeconds = mutableLongStateOf(25 * 60L)

    val focusSeconds = mutableLongStateOf(0L)
    val distractionSeconds = mutableLongStateOf(0L)
    val pauseAllowanceSeconds = mutableLongStateOf(0L)

    // --- INTERNAL HELPERS ---
    private var timerJob: Job? = null
    private var pauseJob: Job? = null
    private var profileListener: ListenerRegistration? = null
    private var questListener: ListenerRegistration? = null

    // --- QUIZ & REWARD STATE ---
    val sessionSummary = mutableStateOf("")
    val isGeneratingQuiz = mutableStateOf(false)
    val generatedQuiz = mutableStateOf<List<QuizQuestion>>(emptyList())
    val lastQuizResults = mutableStateOf<List<UserQuizResult>>(emptyList())
    val currentXpReward = mutableIntStateOf(0)
    val currentCoinReward = mutableIntStateOf(0)

    // --- RANK LOGIC ---
    val ranks = listOf(
        RankDefinition("Wood I", 0, R.drawable.rank_wood_1),
        RankDefinition("Wood II", 100, R.drawable.rank_wood_2),
        RankDefinition("Wood III", 200, R.drawable.rank_wood_3),
        RankDefinition("Bronze I", 300, R.drawable.rank_bronze_1),
        RankDefinition("Bronze II", 400, R.drawable.rank_bronze_2),
        RankDefinition("Bronze III", 500, R.drawable.rank_bronze_3),
        RankDefinition("Silver I", 600, R.drawable.rank_silver_1),
        RankDefinition("Silver II", 800, R.drawable.rank_silver_2),
        RankDefinition("Silver III", 1000, R.drawable.rank_silver_3),
        RankDefinition("Gold I", 1500, R.drawable.rank_gold_1),
        RankDefinition("Gold II", 2000, R.drawable.rank_gold_2),
        RankDefinition("Gold III", 2500, R.drawable.rank_gold_3),
        RankDefinition("Master I", 4000, R.drawable.rank_master_1),
        RankDefinition("Master II", 6000, R.drawable.rank_master_2),
        RankDefinition("Master III", 8000, R.drawable.rank_master_3)
    )

    fun getCurrentRankIndex(): Int {
        val xp = userProfile.value?.xp ?: 0
        val index = ranks.indexOfLast { it.xpRequired <= xp }
        return if (index == -1) 0 else index
    }

    fun getRankProgress(): Float {
        val xp = userProfile.value?.xp ?: 0
        val currentIndex = getCurrentRankIndex()
        if (currentIndex >= ranks.size - 1) return 1.0f
        val currentRankXp = ranks[currentIndex].xpRequired
        val nextRankXp = ranks[currentIndex + 1].xpRequired
        val diff = nextRankXp - currentRankXp
        val progress = xp - currentRankXp
        return (progress.toFloat() / diff.toFloat()).coerceIn(0f, 1f)
    }

    // ==================== INITIALIZATION ====================

    fun startListening() {
        if (userId.isEmpty()) return

        profileListener?.remove()
        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val rawProfile = snapshot.toObject(UserProfile::class.java)
                    val xp = snapshot.getLong("xp")?.toInt() ?: 0
                    val coins = snapshot.getLong("coins")?.toInt() ?: 0
                    val streak = snapshot.getLong("streakDays")?.toInt() ?: 0
                    userProfile.value = rawProfile?.copy(xp = xp, coins = coins, streakDays = streak)
                }
            }

        questListener?.remove()
        questListener = db.collection("users").document(userId).collection("quests")
            .whereEqualTo("status", 0)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    activeQuests.value = snapshot.toObjects(Quest::class.java)
                }
            }
    }

    // ==================== TIMER LOGIC (FIXED) ====================

    fun startSession(context: Context) {
        if (selectedSubQuestIds.value.isEmpty()) {
            Toast.makeText(context, "Select at least one sub-task!", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Calculate duration in seconds
        val durationSeconds = (selectedDurationMin.value.toInt() * 60).toLong()
        totalTimeSeconds.longValue = durationSeconds

        // 2. Set the ABSOLUTE end time (Current Time + Duration)
        // This ensures if user switches tabs or exits, the target time doesn't change
        sessionEndTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000)

        timeRemaining.longValue = durationSeconds
        pauseAllowanceSeconds.longValue = durationSeconds / 12 // 5 mins pause for 60 mins work
        focusSeconds.longValue = 0
        distractionSeconds.longValue = 0

        state.value = SessionState.RUNNING
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        pauseJob?.cancel()

        timerJob = viewModelScope.launch {
            while (state.value == SessionState.RUNNING) {
                val now = System.currentTimeMillis()
                val remainingMillis = sessionEndTimeMillis - now

                if (remainingMillis <= 0) {
                    timeRemaining.longValue = 0
                    state.value = SessionState.COMPLETION_SELECT
                    break
                } else {
                    timeRemaining.longValue = remainingMillis / 1000
                    // Calculate focus time as (Total - Remaining)
                    focusSeconds.longValue = totalTimeSeconds.longValue - timeRemaining.longValue
                }
                delay(1000) // Update UI every second
            }
        }
    }

    fun pauseSession() {
        state.value = SessionState.PAUSED
        timerJob?.cancel()

        // Save how much time was left so we can add it back on resume
        pauseRemainingMillis = sessionEndTimeMillis - System.currentTimeMillis()

        pauseJob = viewModelScope.launch {
            while(pauseAllowanceSeconds.longValue > 0 && state.value == SessionState.PAUSED) {
                delay(1000)
                pauseAllowanceSeconds.longValue--
            }
            if (pauseAllowanceSeconds.longValue <= 0 && state.value == SessionState.PAUSED) {
                resumeSession()
            }
        }
    }

    fun resumeSession() {
        state.value = SessionState.RUNNING
        // Reset the end time based on how much time was remaining
        sessionEndTimeMillis = System.currentTimeMillis() + pauseRemainingMillis
        startTimerLoop()
    }

    fun abandonSession() {
        state.value = SessionState.IDLE
        timerJob?.cancel()
        pauseJob?.cancel()
        selectedSubQuestIds.value = emptySet()
    }

    // Called when user returns to app/tab
    fun onAppForegrounded() {
        // If we were running, we simply restart the loop.
        // Because we use sessionEndTimeMillis (Absolute Time), the timer will
        // "jump" to the correct time immediately, appearing as if it ran in background.
        if (state.value == SessionState.RUNNING) {
            startTimerLoop()
        }
    }

    fun onAppBackgrounded() {
        // No complex logic needed anymore due to absolute timestamp approach
    }

    // ==================== TASK & QUIZ LOGIC ====================

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

    fun generateQuiz(context: Context) {
        isGeneratingQuiz.value = true
        viewModelScope.launch {
            val jsonString = GeminiService.generateQuizQuestion(sessionSummary.value)
            val quiz = parseQuizJson(jsonString)
            if (quiz.isNotEmpty()) {
                generatedQuiz.value = quiz
                state.value = SessionState.QUIZ
            } else {
                Toast.makeText(context, "AI Failed to generate quiz. Write more details!", Toast.LENGTH_SHORT).show()
            }
            isGeneratingQuiz.value = false
        }
    }

    fun completeQuiz(results: List<UserQuizResult>) {
        lastQuizResults.value = results
        val score = results.count { it.selectedOptionIndex == it.question.correctIndex }

        // Rewards Logic
        val minutes = focusSeconds.longValue / 60.0
        val xpCalc = (0.95 * minutes) + (1.0 * score)
        val coinCalc = (1.0 * minutes) + (1.5 * score)
        val newXpEarned = xpCalc.roundToInt()
        val newCoinsEarned = coinCalc.roundToInt()

        currentXpReward.intValue = newXpEarned
        currentCoinReward.intValue = newCoinsEarned

        val currentTotalXp = userProfile.value?.xp ?: 0
        val currentTotalCoins = userProfile.value?.coins ?: 0

        // --- OPTIMISTIC UPDATE (UI Fix) ---
        // Immediately filter out completed quests from the local UI list
        // so they disappear while the server is updating.
        val currentActive = activeQuests.value.toMutableList()
        val iterator = currentActive.iterator()
        while (iterator.hasNext()) {
            val quest = iterator.next()
            val relevantSubs = quest.subQuests.filter { completedSubQuestIds.value.contains(it.id) }

            if (relevantSubs.isNotEmpty()) {
                // Simulate the update locally
                val updatedSubQuests = quest.subQuests.map {
                    if (completedSubQuestIds.value.contains(it.id)) it.copy(isCompleted = true) else it
                }
                // If all subquests are now done, remove this quest from the active list
                if (updatedSubQuests.all { it.isCompleted }) {
                    iterator.remove()
                }
            }
        }
        activeQuests.value = currentActive
        // ----------------------------------

        viewModelScope.launch(Dispatchers.IO) {
            val batch = db.batch()

            // Re-fetch original list for safe DB update
            // We use the original activeQuests logic logic to build the batch
            db.collection("users").document(userId).collection("quests")
                .whereEqualTo("status", 0)
                .get()
                .await()
                .documents.forEach { doc ->
                    val quest = doc.toObject(Quest::class.java)
                    if (quest != null) {
                        val updatedSubQuests = quest.subQuests.map {
                            if (completedSubQuestIds.value.contains(it.id)) it.copy(isCompleted = true) else it
                        }

                        val isQuestDone = updatedSubQuests.all { it.isCompleted }
                        val ref = db.collection("users").document(userId).collection("quests").document(quest.id)

                        batch.update(ref, "subQuests", updatedSubQuests)
                        if (isQuestDone) {
                            batch.update(ref, "status", 1) // Mark as completed on Server
                        }
                    }
                }

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
        lastQuizResults.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        questListener?.remove()
    }
}

// ==================== HELPER FUNCTIONS ====================

fun parseQuizJson(jsonResponse: String): List<QuizQuestion> {
    val questionsList = mutableListOf<QuizQuestion>()
    try {
        var cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim()
        if (cleanJson.startsWith("[")) {
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                questionsList.add(parseObj(obj))
            }
        } else if (cleanJson.startsWith("{")) {
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


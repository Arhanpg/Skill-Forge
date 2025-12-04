package com.skill_forge.app.ui.main.models

import androidx.compose.ui.graphics.Color
import com.skill_forge.app.R
import java.util.UUID

// ==================== ENUMS ====================

enum class SessionState {
    IDLE, RUNNING, PAUSED, COMPLETION_SELECT, REPORTING, QUIZ, REWARD
}

enum class QuestDifficulty(val displayName: String, val xpReward: Int, val color: Color) {
    EASY("Easy", 100, Color(0xFF66BB6A)),     // Green
    MEDIUM("Medium", 250, Color(0xFFFFA726)), // Orange
    HARD("Hard", 500, Color(0xFFEF5350))      // Red
}

// ==================== USER PROFILE ====================
data class UserProfile(
    val username: String = "",
    val email: String = "",
    val xp: Int = 0,
    val coins: Int = 0,
    val streakDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val avatarResourceId: Int? = null,
    val avatarUri: String? = null,
    val dob: String = "",
    val qualification: String = ""
)

data class ServerRequest(val topic: String)

// ==================== RANKS ====================
data class RankTier(
    val name: String,
    val minXp: Int,
    val drawableId: Int,
    val color: Color
)

val ALL_RANKS = listOf(
    RankTier("Wood I", 0, R.drawable.rank_wood_1, Color(0xFF8D6E63)),
    RankTier("Bronze I", 300, R.drawable.rank_bronze_1, Color(0xFFCD7F32)),
    RankTier("Silver I", 600, R.drawable.rank_silver_1, Color(0xFFC0C0C0)),
    RankTier("Gold I", 1000, R.drawable.rank_gold_1, Color(0xFFFFD700)),
    RankTier("Master I", 1500, R.drawable.rank_master_1, Color(0xFF9C27B0))
)

object RankHelper {
    fun getRankDrawable(coins: Long): Int {
        return when {
            coins < 50 -> R.drawable.rank_wood_1
            coins < 100 -> R.drawable.rank_wood_2
            coins < 150 -> R.drawable.rank_wood_3
            coins < 250 -> R.drawable.rank_bronze_1
            coins < 350 -> R.drawable.rank_bronze_2
            coins < 450 -> R.drawable.rank_bronze_3
            coins < 600 -> R.drawable.rank_silver_1
            coins < 750 -> R.drawable.rank_silver_2
            coins < 900 -> R.drawable.rank_silver_3
            coins < 1200 -> R.drawable.rank_gold_1
            coins < 1500 -> R.drawable.rank_gold_2
            coins < 2000 -> R.drawable.rank_gold_3
            coins < 3000 -> R.drawable.rank_master_1
            else -> R.drawable.rank_master_3
        }
    }
}

// ==================== QUESTS & SUBQUESTS ====================

data class SubQuest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    var isCompleted: Boolean = false,
    var isSelectedForSession: Boolean = false
) {
    // No-argument constructor for Firebase
    constructor() : this(UUID.randomUUID().toString(), "", false, false)
}

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    // We store difficulty as a String in Firebase to avoid Enum serialization crashes
    val difficulty: String = QuestDifficulty.EASY.name,
    val subQuests: List<SubQuest> = emptyList(),
    val status: Int = 0, // 0: Active, 1: Completed
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firebase
    constructor() : this(UUID.randomUUID().toString(), "", QuestDifficulty.EASY.name, emptyList(), 0, System.currentTimeMillis())

    // Helper to get the actual Enum within the app code
    val difficultyEnum: QuestDifficulty
        get() = try {
            QuestDifficulty.valueOf(difficulty)
        } catch (e: Exception) {
            QuestDifficulty.EASY
        }

    val progress: Float
        get() = if (subQuests.isEmpty()) 0f else subQuests.count { it.isCompleted }.toFloat() / subQuests.size
}

// ==================== QUIZ MODELS ====================

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
) {
    constructor() : this("", emptyList(), 0)
}

// Result history for the Rewards Screen
data class UserQuizResult(
    val question: QuizQuestion,
    val selectedOptionIndex: Int
)


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
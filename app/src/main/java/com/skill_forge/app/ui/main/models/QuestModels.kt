package com.skill_forge.app.ui.main.models

import androidx.compose.ui.graphics.Color
import com.skill_forge.app.R
import java.util.UUID

// ==================== ENUMS ====================

enum class QuestDifficulty(val displayName: String, val xpReward: Int, val color: Color) {
    EASY("Easy", 100, Color(0xFF66BB6A)),     // Green
    MEDIUM("Medium", 250, Color(0xFFFFA726)), // Orange
    HARD("Hard", 500, Color(0xFFEF5350))      // Red
}

enum class SessionState {
    IDLE, RUNNING, PAUSED, COMPLETION_SELECT, REPORTING, QUIZ, REWARD
}

// ==================== SHARED DATA CLASSES ====================

data class SubQuest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    var isCompleted: Boolean = false,
    var isSelectedForSession: Boolean = false
) {
    // No-argument constructor for Firebase
    constructor() : this(UUID.randomUUID().toString(), "", false)
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

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
) {
    constructor() : this("", emptyList(), 0)
}



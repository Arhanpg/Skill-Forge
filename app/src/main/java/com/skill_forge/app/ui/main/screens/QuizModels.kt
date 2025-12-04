package com.skill_forge.app.ui.main.models

import androidx.compose.ui.graphics.Color
import com.skill_forge.app.R

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
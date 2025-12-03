package com.skill_forge.app.ui.main.models

data class UserProfile(
    // Existing fields
    val username: String = "",
    val email: String = "",
    val xp: Int = 0,
    val coins: Int = 0,
    val streakDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),

    // NEW FIELDS required by your Dialogs
    val avatarResourceId: Int? = null, // For preset avatars (R.drawable.xxx)
    val avatarUri: String? = null,     // For gallery images (content://...)
    val dob: String = "",              // Date of Birth
    val qualification: String = ""     // Qualification
)
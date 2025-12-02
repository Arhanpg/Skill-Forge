package com.example.skill_forge.ui.main.models

data class UserProfile(
    val name: String = "",
    val dob: String = "",
    val qualification: String = "",
    val avatarResourceId: Int? = null,
    val avatarUri: String? = null
)

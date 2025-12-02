package com.example.skill_forge

import android.app.Application
import com.google.firebase.FirebaseApp

class SkillForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase only - NO offline persistence for auth
        FirebaseApp.initializeApp(this)
    }
}

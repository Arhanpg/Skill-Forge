package com.skill_forge.app.utils

import android.util.Log
import com.skill_forge.app.ui.main.models.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

// This single file handles parsing for HomeViewModel AND QuizScreen
object QuizUtils {

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

    private fun parseObj(json: JSONObject): QuizQuestion {
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
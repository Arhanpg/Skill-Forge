package com.skill_forge.app.ui.main.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skill_forge.app.ui.main.models.QuizQuestion

@Composable
fun QuizSessionUI(
    questions: List<QuizQuestion>,
    primaryColor: Color,
    onComplete: (Int) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableIntStateOf(-1) }

    if (questions.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No questions generated.", color = Color.Red)
            Button(onClick = { onComplete(0) }) { Text("Skip") }
        }
        return
    }

    val currentQ = questions[index]

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("⚔️ KNOWLEDGE DUEL", color = Color(0xFFFFD700), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = primaryColor,
            trackColor = Color.Gray.copy(alpha = 0.3f),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Q${index + 1}: ${currentQ.question}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))

        currentQ.options.forEachIndexed { i, opt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { selectedOptionIndex = i },
                colors = CardDefaults.cardColors(containerColor = if (selectedOptionIndex == i) primaryColor else Color(0xFF2D2D2D)),
                border = BorderStroke(1.dp, if (selectedOptionIndex == i) primaryColor else Color.Gray.copy(0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(opt, modifier = Modifier.padding(16.dp), color = if (selectedOptionIndex == i) Color.Black else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (selectedOptionIndex == currentQ.correctIndex) score++
                if (index < questions.size - 1) {
                    index++
                    selectedOptionIndex = -1
                } else {
                    onComplete(score)
                }
            },
            enabled = selectedOptionIndex != -1,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text(if (index < questions.size - 1) "NEXT ATTACK ⚔️" else "FINISH BATTLE 🏆", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
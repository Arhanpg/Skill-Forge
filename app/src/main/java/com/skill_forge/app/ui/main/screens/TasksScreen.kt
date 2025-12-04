package com.skill_forge.app.ui.main.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Import Network Service
import com.skill_forge.app.network.GeminiService

// Import Models
import com.skill_forge.app.ui.main.models.*

import kotlinx.coroutines.launch

@Composable
fun TaskScreen() {
    // FIX: Use the specific "skillforge" database instance
    val db = remember { FirebaseFirestore.getInstance("skillforge") }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for Quests
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Completed
    var showNewQuestDialog by remember { mutableStateOf(false) }

    // State for Quiz Logic
    var showQuizContextDialog by remember { mutableStateOf(false) }
    var showQuizInterface by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }

    // Data holding for the active quiz flow
    var activeQuestForQuiz by remember { mutableStateOf<Quest?>(null) }
    var generatedQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }

    // Firebase Listener
    DisposableEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val listener = db.collection("users").document(userId).collection("quests")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                quests = snapshot?.documents?.mapNotNull { it.toObject(Quest::class.java) } ?: emptyList()
                isLoading = false
            }
        onDispose { listener.remove() }
    }

    val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))

    Box(Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("QUEST BOARD", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            }

            // Tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp).background(Color.Black.copy(0.3f), CircleShape).padding(4.dp)) {
                listOf("Active", "Completed").forEachIndexed { index, title ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (selectedTab == index) Color(0xFF00E5FF) else Color.Transparent).clickable { selectedTab = index }, contentAlignment = Alignment.Center) {
                        Text(title, color = if (selectedTab == index) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // List
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00E5FF)) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val filtered = quests.filter { if (selectedTab == 0) it.status == 0 else it.status == 1 }
                    items(filtered, key = { it.id }) { quest ->
                        QuestCard(
                            quest = quest,
                            userId = userId,
                            db = db,
                            onAttemptCompletion = { q ->
                                // Start the completion flow
                                activeQuestForQuiz = q
                                showQuizContextDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Add Button
        FloatingActionButton(
            onClick = { showNewQuestDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF00E5FF)
        ) { Icon(Icons.Default.Add, "Add") }

        // ================= DIALOGS & OVERLAYS =================

        // 1. Create New Quest Dialog
        if (showNewQuestDialog) {
            QuickQuestDialog(
                onDismiss = { showNewQuestDialog = false },
                onQuestCreated = { q ->
                    if(userId.isNotEmpty()) {
                        db.collection("users").document(userId).collection("quests").document(q.id).set(q)
                    }
                    showNewQuestDialog = false
                }
            )
        }

        // 2. Context Dialog (User enters description for quiz)
        if (showQuizContextDialog && activeQuestForQuiz != null) {
            QuizContextInputDialog(
                questTitle = activeQuestForQuiz!!.title,
                onDismiss = { showQuizContextDialog = false },
                onConfirm = { description ->
                    showQuizContextDialog = false
                    isAiLoading = true

                    // Call AI
                    scope.launch {
                        try {
                            // Using generateTaskQuiz which returns List<QuizQuestion>
                            val questions = GeminiService.generateTaskQuiz(description)

                            if (questions.isNotEmpty()) {
                                generatedQuestions = questions
                                showQuizInterface = true
                            } else {
                                Toast.makeText(context, "AI failed to generate quiz. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isAiLoading = false
                        }
                    }
                }
            )
        }

        // 3. AI Loading State
        if (isAiLoading) {
            Dialog(onDismissRequest = {}) {
                Card(colors = CardDefaults.cardColors(Color(0xFF1E1E1E))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFAB00))
                        Spacer(Modifier.height(16.dp))
                        Text("Forging Quiz...", color = Color.White)
                    }
                }
            }
        }

        // 4. Actual Quiz Interface
        if (showQuizInterface && activeQuestForQuiz != null) {
            QuizDialog(
                questions = generatedQuestions,
                onDismiss = { showQuizInterface = false },
                onSuccess = {
                    showQuizInterface = false
                    val quest = activeQuestForQuiz!!

                    // UPDATE DB: Just Mark Completed (NO XP GIVEN)
                    val userRef = db.collection("users").document(userId)
                    val questRef = userRef.collection("quests").document(quest.id)

                    val batch = db.batch()

                    // 1. Mark quest as completed
                    batch.update(questRef, "status", 1)

                    // XP REWARD REMOVED AS REQUESTED
                    // batch.update(userRef, "xp", FieldValue.increment(quest.difficultyEnum.xpReward.toLong()))

                    batch.commit().addOnSuccessListener {
                        Toast.makeText(context, "Quest Completed!", Toast.LENGTH_SHORT).show()
                        activeQuestForQuiz = null
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to update progress", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun QuestCard(
    quest: Quest,
    userId: String,
    db: FirebaseFirestore,
    onAttemptCompletion: (Quest) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Difficulty Icon
                Box(Modifier.size(48.dp).background(quest.difficultyEnum.color.copy(0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(
                        text = if(quest.status == 1) "✓" else "!",
                        fontSize = 24.sp,
                        color = quest.difficultyEnum.color
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${quest.difficultyEnum.displayName}", // Removed XP display from subtitle as well since no XP is given
                        color = quest.difficultyEnum.color,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { db.collection("users").document(userId).collection("quests").document(quest.id).delete() }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray)
                }
            }

            // Progress Bar
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { quest.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = quest.difficultyEnum.color,
                trackColor = Color.White.copy(0.1f)
            )

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                // Subtasks List
                quest.subQuests.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Checkbox(
                            checked = sub.isCompleted,
                            onCheckedChange = { isChecked ->
                                val updatedSubs = quest.subQuests.map { if (it.id == sub.id) it.copy(isCompleted = isChecked) else it }
                                db.collection("users").document(userId).collection("quests").document(quest.id)
                                    .update("subQuests", updatedSubs)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = quest.difficultyEnum.color)
                        )
                        Text(
                            sub.title,
                            color = if (sub.isCompleted) Color.Gray else Color.White,
                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null
                        )
                    }
                }

                // Completion Button (Only if Active)
                if (quest.status == 0) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onAttemptCompletion(quest) },
                        colors = ButtonDefaults.buttonColors(containerColor = quest.difficultyEnum.color),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify & Complete Quest", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickQuestDialog(onDismiss: () -> Unit, onQuestCreated: (Quest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subTasksText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(QuestDifficulty.EASY) }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(Color(0xFF263238))) {
            Column(Modifier.padding(24.dp)) {
                Text("New Quest", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                Spacer(Modifier.height(8.dp))

                // Difficulty Selection
                Text("Difficulty", color = Color.Gray, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    QuestDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = diff.color,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subTasksText, onValueChange = { subTasksText = it }, label = { Text("Subtasks (one per line)") }, minLines = 3)

                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val subs = subTasksText.split("\n").filter { it.isNotBlank() }.map { SubQuest(title = it.trim()) }
                        onQuestCreated(Quest(title = title, subQuests = subs, difficulty = selectedDifficulty.name))
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Create Quest") }
            }
        }
    }
}

@Composable
fun QuizContextInputDialog(questTitle: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var description by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(Color(0xFF1E1E1E))) {
            Column(Modifier.padding(24.dp)) {
                Text("Proof of Knowledge", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Describe what you learned in '$questTitle' to generate a quiz.", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("e.g., I learned about for-loops in Kotlin...") }
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if(description.isNotBlank()) onConfirm(description) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Quiz", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun QuizDialog(questions: List<QuizQuestion>, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(Color(0xFF263238)), modifier = Modifier.height(400.dp)) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isFinished) {
                    val q = questions.getOrNull(currentQuestionIndex)
                    if (q != null) {
                        Text("Question ${currentQuestionIndex + 1}/${questions.size}", color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text(q.question, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        q.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = {
                                    if (index == q.correctIndex) score++
                                    if (currentQuestionIndex < questions.size - 1) {
                                        currentQuestionIndex++
                                    } else {
                                        isFinished = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(option, color = Color.White)
                            }
                        }
                    }
                } else {
                    // Results
                    val passed = score >= (questions.size / 2) // Pass if >= 50%
                    Text(if (passed) "VICTORY!" else "FAILED", color = if (passed) Color.Green else Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Score: $score/${questions.size}", color = Color.White)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (passed) onSuccess() else onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (passed) Color.Green else Color.Red)
                    ) {
                        Text(if (passed) "Complete Quest" else "Try Again Later", color = Color.Black)
                    }
                }
            }
        }
    }
}
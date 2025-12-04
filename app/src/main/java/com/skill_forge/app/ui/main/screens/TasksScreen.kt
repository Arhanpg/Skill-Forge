package com.skill_forge.app.ui.main.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.skill_forge.app.network.GeminiService
import com.skill_forge.app.ui.main.models.*
import kotlinx.coroutines.launch

@Composable
fun TaskScreen() {
    val db = remember { FirebaseFirestore.getInstance("skillforge") }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- CYBER THEME COLORS ---
    val cyberBlue = Color(0xFF00E5FF)
    val cyberPurple = Color(0xFFD500F9)
    val deepBg = Brush.verticalGradient(listOf(Color(0xFF0B0F19), Color(0xFF162238)))
    val cardBg = Color(0xFF131B29)

    // State
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Completed
    var showNewQuestDialog by remember { mutableStateOf(false) }

    // Quiz State
    var showQuizContextDialog by remember { mutableStateOf(false) }
    var showQuizInterface by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }
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

    Box(
        Modifier
            .fillMaxSize()
            .background(deepBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // --- HEADER ---
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    "QUEST BOARD",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    "Manage your missions",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // --- CYBER TABS ---
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(4.dp)
            ) {
                listOf("ACTIVE", "COMPLETED").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(
                                    listOf(
                                        cyberBlue.copy(alpha = 0.2f),
                                        cyberPurple.copy(alpha = 0.2f)
                                    )
                                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) cyberBlue.copy(0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (isSelected) cyberBlue else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LIST ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cyberBlue)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val filtered = quests.filter { if (selectedTab == 0) it.status == 0 else it.status == 1 }
                    items(filtered, key = { it.id }) { quest ->
                        CyberQuestCard(
                            quest = quest,
                            userId = userId,
                            db = db,
                            onAttemptCompletion = { q ->
                                activeQuestForQuiz = q
                                showQuizContextDialog = true
                            }
                        )
                    }
                }
            }
        }

        // --- FAB ---
        FloatingActionButton(
            onClick = { showNewQuestDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            // Custom Gradient FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape, spotColor = cyberBlue)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(cyberBlue, cyberPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add", tint = Color.White)
            }
        }

        // ================= DIALOGS =================

        if (showNewQuestDialog) {
            CyberDialog(onDismiss = { showNewQuestDialog = false }) {
                QuickQuestContent(
                    onDismiss = { showNewQuestDialog = false },
                    onQuestCreated = { q ->
                        if (userId.isNotEmpty()) {
                            db.collection("users").document(userId).collection("quests").document(q.id).set(q)
                        }
                        showNewQuestDialog = false
                    }
                )
            }
        }

        if (showQuizContextDialog && activeQuestForQuiz != null) {
            CyberDialog(onDismiss = { showQuizContextDialog = false }) {
                QuizContextContent(
                    questTitle = activeQuestForQuiz!!.title,
                    onConfirm = { description ->
                        showQuizContextDialog = false
                        isAiLoading = true
                        scope.launch {
                            try {
                                val questions = GeminiService.generateTaskQuiz(description)
                                if (questions.isNotEmpty()) {
                                    generatedQuestions = questions
                                    showQuizInterface = true
                                } else {
                                    Toast.makeText(context, "AI failed to generate quiz.", Toast.LENGTH_SHORT).show()
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
        }

        if (isAiLoading) {
            Dialog(onDismissRequest = {}) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cyberBlue.copy(0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = cyberBlue)
                        Spacer(Modifier.height(16.dp))
                        Text("FORGING QUIZ...", color = cyberBlue, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }

        if (showQuizInterface && activeQuestForQuiz != null) {
            CyberDialog(onDismiss = { showQuizInterface = false }) {
                QuizContent(
                    questions = generatedQuestions,
                    onDismiss = { showQuizInterface = false },
                    onSuccess = {
                        showQuizInterface = false
                        val quest = activeQuestForQuiz!!
                        val batch = db.batch()
                        val questRef = db.collection("users").document(userId).collection("quests").document(quest.id)

                        // Update status to completed (1)
                        batch.update(questRef, "status", 1)
                        // Also mark all subquests as true just in case
                        val completedSubs = quest.subQuests.map { it.copy(isCompleted = true) }
                        batch.update(questRef, "subQuests", completedSubs)

                        batch.commit().addOnSuccessListener {
                            Toast.makeText(context, "Quest Completed!", Toast.LENGTH_SHORT).show()
                            activeQuestForQuiz = null
                        }
                    }
                )
            }
        }
    }
}

// ==================== CYBER COMPONENTS ====================

@Composable
fun CyberDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 650.dp) // Dynamic height with max limit
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9))),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B0F19))
                .padding(2.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberQuestCard(
    quest: Quest,
    userId: String,
    db: FirebaseFirestore,
    onAttemptCompletion: (Quest) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val cyberBlue = Color(0xFF00E5FF)

    // Status colors
    val statusColor = if (quest.status == 1) Color(0xFF00E676) else quest.difficultyEnum.color

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131B29))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Difficulty Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .shadow(8.dp, CircleShape, spotColor = statusColor)
                )

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        quest.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        quest.difficultyEnum.displayName.uppercase(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = { db.collection("users").document(userId).collection("quests").document(quest.id).delete() }) {
                    Icon(Icons.Default.Delete, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(20.dp))
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = cyberBlue,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Progress Bar
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { quest.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = Color.White.copy(0.05f)
            )

            if (expanded) {
                Spacer(Modifier.height(16.dp))

                // Subtasks List
                quest.subQuests.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(0.2f))
                            .padding(8.dp)
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = { isChecked ->
                                    val updatedSubs = quest.subQuests.map { if (it.id == sub.id) it.copy(isCompleted = isChecked) else it }
                                    db.collection("users").document(userId).collection("quests").document(quest.id)
                                        .update("subQuests", updatedSubs)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = statusColor,
                                    uncheckedColor = Color.Gray
                                )
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            sub.title,
                            color = if (sub.isCompleted) Color.Gray else Color.White.copy(0.9f),
                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null,
                            fontSize = 14.sp
                        )
                    }
                }

                // Completion Button (Only if Active)
                if (quest.status == 0) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onAttemptCompletion(quest) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(cyberBlue, Color(0xFF2979FF)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VERIFY & COMPLETE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== DIALOG CONTENTS ====================

@Composable
fun QuickQuestContent(onDismiss: () -> Unit, onQuestCreated: (Quest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subTasksText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(QuestDifficulty.EASY) }
    val cyberBlue = Color(0xFF00E5FF)

    Column(Modifier.padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NEW QUEST", color = cyberBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
        }

        Spacer(Modifier.height(16.dp))

        CyberTextField(value = title, onValueChange = { title = it }, label = "Quest Title")

        Spacer(Modifier.height(16.dp))

        // Difficulty Selection
        Text("DIFFICULTY", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuestDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = diff },
                    label = { Text(diff.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = diff.color.copy(alpha = 0.2f),
                        selectedLabelColor = diff.color,
                        containerColor = Color.Transparent,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedDifficulty == diff,
                        borderColor = Color.Gray.copy(0.3f),
                        selectedBorderColor = diff.color
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        CyberTextField(
            value = subTasksText,
            onValueChange = { subTasksText = it },
            label = "Subtasks (one per line)",
            minLines = 3
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val subs = subTasksText.split("\n").filter { it.isNotBlank() }.map { SubQuest(title = it.trim()) }
                    onQuestCreated(Quest(title = title, subQuests = subs, difficulty = selectedDifficulty.name))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cyberBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CREATE QUEST", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuizContextContent(questTitle: String, onConfirm: (String) -> Unit) {
    var description by remember { mutableStateOf("") }
    val cyberBlue = Color(0xFF00E5FF)

    Column(Modifier.padding(24.dp)) {
        Text("KNOWLEDGE CHECK", color = cyberBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text("To complete '$questTitle', describe what you learned or achieved.", color = Color.White.copy(0.7f), fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))

        CyberTextField(
            value = description,
            onValueChange = { description = it },
            label = "I learned...",
            minLines = 4
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { if(description.isNotBlank()) onConfirm(description) },
            colors = ButtonDefaults.buttonColors(containerColor = cyberBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("INITIATE QUIZ", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuizContent(questions: List<QuizQuestion>, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    // Track user answers: Map<QuestionIndex, SelectedOptionIndex>
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var isFinished by remember { mutableStateOf(false) }
    val cyberBlue = Color(0xFF00E5FF)

    // Calculate score dynamically
    val score = remember(userAnswers) {
        userAnswers.count { (index, selected) ->
            questions.getOrNull(index)?.correctIndex == selected
        }
    }

    Column(
        Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = isFinished,
            transitionSpec = { fadeIn() + slideInVertically { it / 20 } togetherWith fadeOut() + slideOutVertically { -it / 20 } },
            label = "QuizState"
        ) { finished ->
            if (!finished) {
                // --- ACTIVE QUIZ VIEW ---
                val q = questions.getOrNull(currentQuestionIndex)
                if (q != null) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "QUESTION ${currentQuestionIndex + 1}/${questions.size}",
                                color = cyberBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(q.question, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(24.dp))

                        q.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = {
                                    // Save answer
                                    userAnswers[currentQuestionIndex] = index

                                    if (currentQuestionIndex < questions.size - 1) {
                                        currentQuestionIndex++
                                    } else {
                                        isFinished = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    option,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            } else {
                // --- SUMMARY / RESULT VIEW ---
                val passed = score >= (questions.size / 2)
                val resultColor = if (passed) Color(0xFF00E676) else Color(0xFFFF1744)

                Column(
                    modifier = Modifier.fillMaxHeight(), // Take available space
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            if (passed) Icons.Default.Check else Icons.Default.Close,
                            null,
                            tint = resultColor,
                            modifier = Modifier
                                .size(64.dp)
                                .background(resultColor.copy(0.1f), CircleShape)
                                .padding(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (passed) "QUEST COMPLETE" else "QUEST FAILED",
                            color = resultColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text("Score: $score/${questions.size}", color = Color.White.copy(0.7f))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("MISSION REPORT", color = cyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    // Scrollable Summary List
                    Box(
                        modifier = Modifier
                            .weight(1f) // Important for scrolling inside Dialog
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(0.3f))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                    ) {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(questions.size) { index ->
                                val question = questions[index]
                                val userSelected = userAnswers[index] ?: -1
                                val isCorrect = userSelected == question.correctIndex
                                val correctText = question.options.getOrElse(question.correctIndex) { "" }
                                val userText = question.options.getOrElse(userSelected) { "Skipped" }

                                Column {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            if(isCorrect) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            null,
                                            tint = if(isCorrect) Color(0xFF00E676) else Color(0xFFFF1744),
                                            modifier = Modifier.size(16.dp).offset(y = 2.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Q${index+1}: ${question.question}",
                                            color = Color.White.copy(0.9f),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // User Answer
                                    Text(
                                        "You: $userText",
                                        color = if(isCorrect) Color(0xFF00E676) else Color(0xFFFF1744),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )

                                    // Correct Answer (if wrong)
                                    if (!isCorrect) {
                                        Text(
                                            "Correct: $correctText",
                                            color = Color(0xFF00E676),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { if (passed) onSuccess() else onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = resultColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (passed) "CLAIM VICTORY" else "RETURN TO BASE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberTextField(value: String, onValueChange: (String) -> Unit, label: String, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color.White.copy(0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00E5FF),
            focusedContainerColor = Color(0xFF131B29),
            unfocusedContainerColor = Color(0xFF131B29)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
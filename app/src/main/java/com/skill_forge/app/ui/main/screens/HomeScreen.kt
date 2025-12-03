package com.skill_forge.app.ui.main.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skill_forge.app.ui.main.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.skill_forge.app.R

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

// ==================== CONFIGURATION ====================

enum class SessionState {
    IDLE, RUNNING, PAUSED, COMPLETION_SELECT, REPORTING, QUIZ, REWARD
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

// Data class to hold Rank Information
data class RankTier(
    val name: String,
    val minXp: Int,
    val drawableId: Int,
    val color: Color
)

// Centralized List of All Ranks
val ALL_RANKS = listOf(
    RankTier("Wood I", 0, R.drawable.rank_wood_1, Color(0xFF8D6E63)),
    RankTier("Wood II", 100, R.drawable.rank_wood_2, Color(0xFF8D6E63)),
    RankTier("Wood III", 200, R.drawable.rank_wood_3, Color(0xFF8D6E63)),

    RankTier("Bronze I", 300, R.drawable.rank_bronze_1, Color(0xFFCD7F32)),
    RankTier("Bronze II", 400, R.drawable.rank_bronze_2, Color(0xFFCD7F32)),
    RankTier("Bronze III", 500, R.drawable.rank_bronze_3, Color(0xFFCD7F32)),

    RankTier("Silver I", 600, R.drawable.rank_silver_1, Color(0xFFC0C0C0)),
    RankTier("Silver II", 700, R.drawable.rank_silver_2, Color(0xFFC0C0C0)),
    RankTier("Silver III", 850, R.drawable.rank_silver_3, Color(0xFFC0C0C0)),

    RankTier("Gold I", 1000, R.drawable.rank_gold_1, Color(0xFFFFD700)),
    RankTier("Gold II", 1150, R.drawable.rank_gold_2, Color(0xFFFFD700)),
    RankTier("Gold III", 1300, R.drawable.rank_gold_3, Color(0xFFFFD700)),

    RankTier("Master I", 1500, R.drawable.rank_master_1, Color(0xFF9C27B0)),
    RankTier("Master II", 1700, R.drawable.rank_master_2, Color(0xFF9C27B0)),
    RankTier("Master III", 2000, R.drawable.rank_master_3, Color(0xFFE040FB))
)

// ==================== MAIN SCREEN ====================

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRankDialog by remember { mutableStateOf(false) }

    // Cyber Theme Colors
    val cyberBlue = Color(0xFF00E5FF)
    val cyberPurple = Color(0xFFD500F9)
    val darkBgStart = Color(0xFF0B0F19)
    val darkBgEnd = Color(0xFF162238)

    // Lifecycle Observer for Distraction Tracking
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            } else if (event == Lifecycle.Event.ON_START) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(darkBgStart, darkBgEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            if (viewModel.userProfile.value != null) {
                HeaderStats(
                    user = viewModel.userProfile.value!!,
                    onRankClick = { showRankDialog = true }
                )
            } else {
                CircularProgressIndicator(color = cyberBlue)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Content Area
            AnimatedContent(targetState = viewModel.state.value, label = "session_state") { state ->
                when (state) {
                    SessionState.IDLE -> IdleState(
                        activeQuests = viewModel.activeQuests.value,
                        selectedSubIds = viewModel.selectedSubQuestIds.value,
                        sliderValue = viewModel.selectedDurationMin.value,
                        onSliderChange = { viewModel.selectedDurationMin.value = it },
                        onSubTaskToggle = { subId -> viewModel.toggleSubTaskSelection(subId) },
                        onStart = { viewModel.startSession(context) },
                        primaryColor = cyberBlue
                    )
                    SessionState.RUNNING -> RunningState(
                        timeRemaining = viewModel.timeRemaining.longValue,
                        totalTime = viewModel.totalTimeSeconds.longValue,
                        focusSeconds = viewModel.focusSeconds.longValue,
                        distractionSeconds = viewModel.distractionSeconds.longValue,
                        onPause = { viewModel.pauseSession() },
                        onAbandon = { viewModel.abandonSession() },
                        primaryColor = cyberBlue
                    )
                    SessionState.PAUSED -> PausedState(
                        allowanceRemaining = viewModel.pauseAllowanceSeconds.longValue,
                        onResume = { viewModel.resumeSession() }
                    )
                    SessionState.COMPLETION_SELECT -> CompletionSelectState(
                        quests = viewModel.activeQuests.value,
                        selectedSubIds = viewModel.selectedSubQuestIds.value,
                        completedSubIds = viewModel.completedSubQuestIds.value,
                        onToggleComplete = { subId -> viewModel.toggleSubTaskCompletion(subId) },
                        onConfirm = { viewModel.confirmCompletion() },
                        primaryColor = cyberBlue
                    )
                    SessionState.REPORTING -> ReportState(
                        sessionSummary = viewModel.sessionSummary.value,
                        onSummaryChange = { viewModel.sessionSummary.value = it },
                        isGenerating = viewModel.isGeneratingQuiz.value,
                        onGenerate = {
                            if (viewModel.sessionSummary.value.trim().split("\\s+".toRegex()).size < 5) {
                                Toast.makeText(context, "Summary too short (min 5 words)!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.generateQuiz(context)
                            }
                        },
                        primaryColor = cyberBlue
                    )
                    SessionState.QUIZ -> QuizState(
                        questions = viewModel.generatedQuiz.value,
                        primaryColor = cyberBlue,
                        onComplete = { score -> viewModel.completeQuiz(score) }
                    )
                    SessionState.REWARD -> RewardState(
                        xp = viewModel.currentXpReward.intValue,
                        coins = viewModel.currentCoinReward.intValue,
                        primaryColor = cyberPurple,
                        onClaim = { viewModel.resetSession() }
                    )
                }
            }
        }

        // --- RANK PROGRESSION DIALOG ---
        if (showRankDialog && viewModel.userProfile.value != null) {
            RankDetailsDialog(
                currentXp = viewModel.userProfile.value!!.xp,
                onDismiss = { showRankDialog = false }
            )
        }
    }
}

// ==================== SUB-COMPONENTS ====================

@Composable
fun HeaderStats(user: UserProfile, onRankClick: () -> Unit) {
    val currentRank = ALL_RANKS.lastOrNull { user.xp >= it.minXp } ?: ALL_RANKS.first()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(icon = "🔥", value = "${user.streakDays}", label = "Streak", color = Color(0xFFFF5722))
        Divider(color = Color.White.copy(0.2f), modifier = Modifier.height(30.dp).width(1.dp))
        StatItem(icon = "⚡", value = "${user.coins}", label = "Edu Coins", color = Color(0xFFFFD700))
        Divider(color = Color.White.copy(0.2f), modifier = Modifier.height(30.dp).width(1.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRankClick() }
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = currentRank.drawableId),
                contentDescription = "Rank Badge",
                modifier = Modifier.size(45.dp)
            )
            Text(text = "${user.xp} XP", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RankDetailsDialog(currentXp: Int, onDismiss: () -> Unit) {
    val maxRankXp = ALL_RANKS.last().minXp + 500
    val progressTarget = (currentXp.toFloat() / maxRankXp.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162238)),
            border = BorderStroke(2.dp, Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9)))),
            modifier = Modifier.fillMaxWidth().height(600.dp).shadow(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RANK PROGRESSION", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Total XP: $currentXp", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(0.5f))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9)))))
                }
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(ALL_RANKS) { index, rank ->
                        val isUnlocked = currentXp >= rank.minXp
                        val isCurrent = isUnlocked && (index == ALL_RANKS.lastIndex || currentXp < ALL_RANKS[index+1].minXp)
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(index * 100L); isVisible = true }
                        AnimatedVisibility(visible = isVisible, enter = slideInHorizontally(initialOffsetX = { 100 }) + fadeIn()) {
                            RankListItem(rank = rank, isUnlocked = isUnlocked, isCurrent = isCurrent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankListItem(rank: RankTier, isUnlocked: Boolean, isCurrent: Boolean) {
    val borderColor = if (isCurrent) Color(0xFF00E5FF) else if (isUnlocked) Color.Green.copy(0.3f) else Color.Gray.copy(0.3f)
    val bgColor = if (isCurrent) Color(0xFF00E5FF).copy(0.1f) else Color.Black.copy(0.3f)
    val alpha = if (isUnlocked) 1f else 0.4f

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if(isCurrent) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = rank.drawableId), contentDescription = null, modifier = Modifier.size(50.dp).alpha(alpha))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rank.name, color = if(isCurrent) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${rank.minXp} XP Required", color = if(isCurrent) Color(0xFF00E5FF) else Color.Gray.copy(0.6f), fontSize = 12.sp)
            }
            if (isCurrent) {
                Text("CURRENT", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else if (isUnlocked) {
                Icon(Icons.Default.CheckBox, null, tint = Color.Green)
            } else {
                Icon(Icons.Default.Lock, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun StatItem(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$icon $value", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun IdleState(
    activeQuests: List<Quest>,
    selectedSubIds: Set<String>,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    onSubTaskToggle: (String) -> Unit,
    onStart: () -> Unit,
    primaryColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚔️ PREPARE FOR BATTLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)),
            border = BorderStroke(1.dp, primaryColor.copy(0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BATTLE DURATION", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${sliderValue.toInt()} min", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = sliderValue,
                    onValueChange = onSliderChange,
                    valueRange = 1f..120f,
                    steps = 119,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("🎯 Select Sub-Tasks to Focus On", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (activeQuests.isEmpty()) {
                item { Text("No active quests. Add one in Tasks tab!", color = Color.Gray, modifier = Modifier.padding(8.dp)) }
            }
            items(activeQuests) { quest ->
                if (quest.subQuests.any { !it.isCompleted }) {
                    Text(quest.title, color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    quest.subQuests.filter { !it.isCompleted }.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp).clickable { onSubTaskToggle(sub.id) }
                        ) {
                            Icon(
                                imageVector = if (selectedSubIds.contains(sub.id)) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (selectedSubIds.contains(sub.id)) primaryColor else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sub.title, color = Color.White, fontSize = 14.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStart,
            enabled = selectedSubIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(28.dp), spotColor = primaryColor),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor, disabledContainerColor = Color.Gray.copy(0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = if(selectedSubIds.isNotEmpty()) Color.Black else Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START BATTLE", color = if(selectedSubIds.isNotEmpty()) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun RunningState(
    timeRemaining: Long,
    totalTime: Long,
    focusSeconds: Long,
    distractionSeconds: Long,
    onPause: () -> Unit,
    onAbandon: () -> Unit,
    primaryColor: Color
) {
    val progress = if (totalTime > 0) timeRemaining.toFloat() / totalTime.toFloat() else 0f
    val mins = timeRemaining / 60
    val secs = timeRemaining % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BATTLE IN PROGRESS", color = primaryColor, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(Color.Gray.copy(0.2f), 0f, 360f, false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
                drawArc(Brush.sweepGradient(listOf(primaryColor, Color(0xFF2979FF))), -90f, 360 * progress, false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(mins, secs), color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold)
                Text("Remaining", color = Color.Gray, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚡ FOCUS", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${focusSeconds / 60}m ${focusSeconds % 60}s", color = Color.White, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💤 DISTRACTION", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${distractionSeconds / 60}m ${distractionSeconds % 60}s", color = Color.White, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onPause, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("⏸️ Pause") }
            Button(onClick = onAbandon, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Text("❌ Abandon") }
        }
    }
}

@Composable
fun PausedState(allowanceRemaining: Long, onResume: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⏸️ PAUSED", color = Color.Yellow, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Resume in: ${allowanceRemaining}s", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("(Limit: Total Time / 12)", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onResume, modifier = Modifier.height(50.dp)) { Text("RESUME BATTLE") }
        }
    }
}

@Composable
fun CompletionSelectState(
    quests: List<Quest>,
    selectedSubIds: Set<String>,
    completedSubIds: Set<String>,
    onToggleComplete: (String) -> Unit,
    onConfirm: () -> Unit,
    primaryColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✅ MISSION DEBRIEF", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Check tasks you actually finished:", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(quests) { quest ->
                val relevantSubtasks = quest.subQuests.filter { selectedSubIds.contains(it.id) }
                if (relevantSubtasks.isNotEmpty()) {
                    Text(quest.title, color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    relevantSubtasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp).clickable { onToggleComplete(sub.id) }
                        ) {
                            Icon(
                                imageVector = if (completedSubIds.contains(sub.id)) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (completedSubIds.contains(sub.id)) Color.Green else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                color = Color.White,
                                textDecoration = if (completedSubIds.contains(sub.id)) TextDecoration.LineThrough else null
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(0.1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("CONFIRM & GENERATE QUIZ", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReportState(sessionSummary: String, onSummaryChange: (String) -> Unit, isGenerating: Boolean, onGenerate: () -> Unit, primaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📝 BATTLE REPORT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = sessionSummary,
            onValueChange = onSummaryChange,
            label = { Text("What did you learn today? (Min 5 words)") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = primaryColor, unfocusedBorderColor = Color.Gray),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
            } else {
                Text("🤖 GENERATE AI QUIZ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuizState(questions: List<QuizQuestion>, primaryColor: Color, onComplete: (Int) -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }

    if (questions.isEmpty()) {
        Text("No questions generated.", color = Color.Red)
        Button(onClick = { onComplete(0) }) { Text("Skip") }
        return
    }
    val currentQ = questions[index]
    Column(modifier = Modifier.padding(16.dp)) {
        Text("⚔️ KNOWLEDGE DUEL", color = Color(0xFFFFD700), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Q${index + 1}: ${currentQ.question}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        currentQ.options.forEachIndexed { i, opt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = i },
                colors = CardDefaults.cardColors(containerColor = if (selected == i) primaryColor else Color(0xFF2D2D2D)),
                border = BorderStroke(1.dp, if (selected == i) primaryColor else Color.Gray.copy(0.3f))
            ) {
                Text(opt, modifier = Modifier.padding(16.dp), color = if (selected == i) Color.Black else Color.White)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (selected == currentQ.correctIndex) score++
                if (index < questions.size - 1) {
                    index++
                    selected = -1
                } else {
                    onComplete(score)
                }
            },
            enabled = selected != -1,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text(if (index < questions.size - 1) "NEXT ATTACK ⚔️" else "FINISH BATTLE 🏆", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RewardState(xp: Int, coins: Int, primaryColor: Color, onClaim: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("🎉 VICTORY! 🎉", fontSize = 36.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)),
            border = BorderStroke(1.dp, primaryColor),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LOOT EARNED", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+$xp XP", color = primaryColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💰", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+$coins Coins", color = Color(0xFFFFD700), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onClaim,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color.Green)
        ) {
            Text("CLAIM REWARDS & HOME", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// ==================== VIEWMODEL ====================

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: ""

    // UI State
    val userProfile = mutableStateOf<UserProfile?>(null)
    val state = mutableStateOf(SessionState.IDLE)
    val activeQuests = mutableStateOf<List<Quest>>(emptyList())

    // Task Selection
    val selectedSubQuestIds = mutableStateOf<Set<String>>(emptySet())
    val completedSubQuestIds = mutableStateOf<Set<String>>(emptySet())

    // Timer State
    val selectedDurationMin = mutableFloatStateOf(25f)
    val totalTimeSeconds = mutableLongStateOf(25 * 60L)
    val timeRemaining = mutableLongStateOf(25 * 60L)
    val focusSeconds = mutableLongStateOf(0L)
    val distractionSeconds = mutableLongStateOf(0L)
    val pauseAllowanceSeconds = mutableLongStateOf(0L)

    private var timerJob: Job? = null
    private var pauseJob: Job? = null
    private var lastBackgroundTimestamp = 0L
    private var profileListener: ListenerRegistration? = null
    private var questListener: ListenerRegistration? = null

    // Quiz & Reward
    val sessionSummary = mutableStateOf("")
    val isGeneratingQuiz = mutableStateOf(false)
    val generatedQuiz = mutableStateOf<List<QuizQuestion>>(emptyList())
    val currentXpReward = mutableIntStateOf(0)
    val currentCoinReward = mutableIntStateOf(0)

    fun startListening() {
        if (userId.isEmpty()) return

        // Real-time Profile
        profileListener?.remove()
        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    val xp = snapshot.getLong("xp")?.toInt() ?: 0
                    val coins = snapshot.getLong("coins")?.toInt() ?: 0
                    val streak = snapshot.getLong("streakDays")?.toInt() ?: 0
                    val rawProfile = snapshot.toObject(UserProfile::class.java)
                    userProfile.value = rawProfile?.copy(xp = xp, coins = coins, streakDays = streak)
                }
            }

        // Real-time Quests
        questListener?.remove()
        questListener = db.collection("users").document(userId).collection("quests")
            .whereEqualTo("status", 0).addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    activeQuests.value = snapshot.toObjects(Quest::class.java)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        questListener?.remove()
    }

    fun toggleSubTaskSelection(subId: String) {
        val current = selectedSubQuestIds.value.toMutableSet()
        if (current.contains(subId)) current.remove(subId) else current.add(subId)
        selectedSubQuestIds.value = current
    }

    fun startSession(context: Context) {
        if (selectedSubQuestIds.value.isEmpty()) {
            Toast.makeText(context, "Select at least one sub-task!", Toast.LENGTH_SHORT).show()
            return
        }
        val duration = (selectedDurationMin.value.toInt() * 60).toLong()
        totalTimeSeconds.longValue = duration
        timeRemaining.longValue = duration
        pauseAllowanceSeconds.longValue = duration / 12
        focusSeconds.longValue = 0
        distractionSeconds.longValue = 0
        state.value = SessionState.RUNNING
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        pauseJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeRemaining.longValue > 0 && state.value == SessionState.RUNNING) {
                delay(1000)
                timeRemaining.longValue--
                focusSeconds.longValue++
            }
            if (timeRemaining.longValue <= 0) {
                state.value = SessionState.COMPLETION_SELECT
            }
        }
    }

    fun pauseSession() {
        state.value = SessionState.PAUSED
        timerJob?.cancel()
        pauseJob = viewModelScope.launch {
            while(pauseAllowanceSeconds.longValue > 0 && state.value == SessionState.PAUSED) {
                delay(1000)
                pauseAllowanceSeconds.longValue--
            }
            if (pauseAllowanceSeconds.longValue <= 0 && state.value == SessionState.PAUSED) {
                resumeSession()
            }
        }
    }

    fun resumeSession() {
        state.value = SessionState.RUNNING
        startTimer()
    }

    fun abandonSession() {
        state.value = SessionState.IDLE
        timerJob?.cancel()
        pauseJob?.cancel()
        selectedSubQuestIds.value = emptySet()
    }

    fun toggleSubTaskCompletion(subId: String) {
        val current = completedSubQuestIds.value.toMutableSet()
        if (current.contains(subId)) current.remove(subId) else current.add(subId)
        completedSubQuestIds.value = current
    }

    fun confirmCompletion() {
        state.value = SessionState.REPORTING
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (state.value == SessionState.RUNNING && lastBackgroundTimestamp != 0L) {
            val now = System.currentTimeMillis()
            val diffSeconds = (now - lastBackgroundTimestamp) / 1000
            if (diffSeconds > 0) {
                distractionSeconds.longValue += diffSeconds
                timeRemaining.longValue = (timeRemaining.longValue - diffSeconds).coerceAtLeast(0)
            }
            lastBackgroundTimestamp = 0L
            if (timeRemaining.longValue <= 0) {
                state.value = SessionState.COMPLETION_SELECT
                timerJob?.cancel()
            }
        }
    }

    fun generateQuiz(context: Context) {
        isGeneratingQuiz.value = true
        viewModelScope.launch {
            val quiz = generateFirebaseQuiz(sessionSummary.value)
            if (quiz != null && quiz.isNotEmpty()) {
                generatedQuiz.value = quiz
                state.value = SessionState.QUIZ
            } else {
                Toast.makeText(context, "AI Failed. Try adding more details.", Toast.LENGTH_SHORT).show()
            }
            isGeneratingQuiz.value = false
        }
    }

    fun completeQuiz(score: Int) {
        val activeMins = focusSeconds.longValue / 60
        val baseXp = activeMins * 10
        val quizBonus = score * 50
        currentXpReward.intValue = (baseXp + quizBonus).toInt()
        currentCoinReward.intValue = activeMins.toInt()

        val newXp = (userProfile.value?.xp ?: 0) + currentXpReward.intValue
        val newCoins = (userProfile.value?.coins ?: 0) + currentCoinReward.intValue

        viewModelScope.launch(Dispatchers.IO) {
            val batch = db.batch()
            activeQuests.value.forEach { quest ->
                val relevantSubs = quest.subQuests.filter { completedSubQuestIds.value.contains(it.id) }
                if (relevantSubs.isNotEmpty()) {
                    val updatedSubQuests = quest.subQuests.map {
                        if (completedSubQuestIds.value.contains(it.id)) it.copy(isCompleted = true) else it
                    }
                    val isQuestDone = updatedSubQuests.all { it.isCompleted }
                    val ref = db.collection("users").document(userId).collection("quests").document(quest.id)
                    batch.update(ref, "subQuests", updatedSubQuests)
                    if (isQuestDone) batch.update(ref, "status", 1)
                }
            }
            try {
                batch.commit().await()
                db.collection("users").document(userId)
                    .set(mapOf("xp" to newXp, "coins" to newCoins), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving", e)
            }
        }
        state.value = SessionState.REWARD
    }

    fun resetSession() {
        state.value = SessionState.IDLE
        focusSeconds.longValue = 0
        distractionSeconds.longValue = 0
        selectedSubQuestIds.value = emptySet()
        completedSubQuestIds.value = emptySet()
        sessionSummary.value = ""
    }
}

// ==================== FIREBASE VERTEX AI LOGIC (FIXED) ====================

/**
 * Updated to handle both JSON Array and JSON Object formats from GeminiService
 */
suspend fun generateFirebaseQuiz(summary: String): List<QuizQuestion>? {
    return withContext(Dispatchers.IO) {
        try {
            val jsonString = GeminiService.generateQuizQuestion(summary)
            // 1. Sanitize Markdown
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()

            val questions = mutableListOf<QuizQuestion>()

            // 2. Check if it is an Array [...] or Object {...}
            if (cleanJson.startsWith("[")) {
                // It's a JSON Array (List of questions)
                val jsonArray = JSONArray(cleanJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    questions.add(parseJsonObjToQuestion(obj))
                }
            } else if (cleanJson.startsWith("{")) {
                // It's a single JSON Object
                val jsonObject = JSONObject(cleanJson)
                questions.add(parseJsonObjToQuestion(jsonObject))
            } else {
                Log.e("AI_QUIZ", "Invalid JSON format: $cleanJson")
                return@withContext null
            }

            questions

        } catch (e: Exception) {
            Log.e("AI_QUIZ", "Gemini Service Parsing Error", e)
            null
        }
    }
}

// Helper function to parse individual JSON object
fun parseJsonObjToQuestion(json: JSONObject): QuizQuestion {
    val questionText = json.getString("question")
    val correctIdx = json.getInt("correctIndex")
    val optionsArray = json.getJSONArray("options")
    val optionsList = mutableListOf<String>()
    for (i in 0 until optionsArray.length()) {
        optionsList.add(optionsArray.getString(i))
    }
    return QuizQuestion(
        question = questionText,
        options = optionsList,
        correctIndex = correctIdx
    )
}
package com.skill_forge.app.ui.main.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skill_forge.app.ui.main.models.*

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRankDialog by remember { mutableStateOf(false) }

    val cyberBlue = Color(0xFF00E5FF)
    val cyberPurple = Color(0xFFD500F9)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onAppBackgrounded()
            else if (event == Lifecycle.Event.ON_START) viewModel.onAppForegrounded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.startListening() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F19), Color(0xFF162238))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER STATS
            if (viewModel.userProfile.value != null) {
                HeaderStats(viewModel.userProfile.value!!, { showRankDialog = true })
            } else {
                CircularProgressIndicator(color = cyberBlue)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // MAIN CONTENT ANIMATION
            AnimatedContent(targetState = viewModel.state.value, label = "state") { state ->
                when (state) {
                    SessionState.IDLE -> IdleState(
                        activeQuests = viewModel.activeQuests.value,
                        selectedSubIds = viewModel.selectedSubQuestIds.value,
                        sliderValue = viewModel.selectedDurationMin.value,
                        onSliderChange = { viewModel.selectedDurationMin.value = it },
                        onSubTaskToggle = { viewModel.toggleSubTaskSelection(it) },
                        onStart = { viewModel.startSession(context) },
                        primaryColor = cyberBlue
                    )
                    SessionState.RUNNING -> RunningState(
                        timeRemaining = viewModel.timeRemaining.longValue,
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
                        onToggleComplete = { viewModel.toggleSubTaskCompletion(it) },
                        onConfirm = { viewModel.confirmCompletion() }
                    )
                    SessionState.REPORTING -> ReportState(
                        sessionSummary = viewModel.sessionSummary.value,
                        onSummaryChange = { viewModel.sessionSummary.value = it },
                        isGenerating = viewModel.isGeneratingQuiz.value,
                        onGenerate = {
                            if (viewModel.sessionSummary.value.length < 5) Toast.makeText(context, "Write more!", Toast.LENGTH_SHORT).show()
                            else viewModel.generateQuiz(context)
                        }
                    )
                    SessionState.QUIZ -> QuizSessionUI(
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

        // --- CUSTOM RANK DIALOG UI ---
        if (showRankDialog && viewModel.userProfile.value != null) {
            RankProgressionDialog(
                currentXp = viewModel.userProfile.value!!.xp,
                ranks = viewModel.ranks,
                currentIndex = viewModel.getCurrentRankIndex(),
                progress = viewModel.getRankProgress(),
                onDismiss = { showRankDialog = false }
            )
        }
    }
}

@Composable
fun RankProgressionDialog(
    currentXp: Int,
    ranks: List<RankDefinition>,
    currentIndex: Int,
    progress: Float,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to current rank automatically
    LaunchedEffect(Unit) {
        listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(350.dp)
                .height(650.dp)
                // Gradient Border effect
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9))),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(Color(0xFF0B0F19), RoundedCornerShape(16.dp))
                .padding(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANK PROGRESSION",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total XP Label
                Text(
                    text = "Total XP: $currentXp",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Linear Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2738))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9))))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable Rank List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(ranks) { index, rank ->
                        val isCurrent = index == currentIndex
                        val isLocked = index > currentIndex
                        val isPassed = index < currentIndex

                        RankItemCard(rank, isCurrent, isLocked, isPassed)
                    }
                }
            }
        }
    }
}

@Composable
fun RankItemCard(rank: RankDefinition, isCurrent: Boolean, isLocked: Boolean, isPassed: Boolean) {
    val borderColor = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF2A3142)
    val bgColor = if (isCurrent) Color(0xFF131B29) else Color(0xFF131B29)
    val alpha = if (isLocked) 0.5f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Icon
        Image(
            painter = painterResource(id = rank.drawableId),
            contentDescription = rank.title,
            modifier = Modifier.size(48.dp).alpha(alpha)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Rank Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rank.title,
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${rank.xpRequired} XP Required",
                color = Color.Gray.copy(alpha = alpha),
                fontSize = 12.sp
            )
        }

        // Status Indicator
        if (isCurrent) {
            Text(
                text = "CURRENT",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        } else if (isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Passed
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Passed",
                tint = Color(0xFFD500F9),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== EXISTING SUB COMPONENTS ====================

@Composable
fun HeaderStats(user: UserProfile, onRankClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(16.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥 ${user.streakDays}", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
            Text("Streak", color = Color.Gray, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚡ ${user.coins}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            Text("Coins", color = Color.Gray, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onRankClick() }) {
            Icon(Icons.Default.Star, null, tint = Color.Magenta)
            Text("${user.xp} XP", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun IdleState(activeQuests: List<Quest>, selectedSubIds: Set<String>, sliderValue: Float, onSliderChange: (Float) -> Unit, onSubTaskToggle: (String) -> Unit, onStart: () -> Unit, primaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PREPARE FOR BATTLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        Text("${sliderValue.toInt()} min", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Slider(value = sliderValue, onValueChange = onSliderChange, valueRange = 1f..120f, colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor))
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.height(200.dp).fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).padding(8.dp)) {
            if (activeQuests.isEmpty()) {
                Text("No active quests.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(activeQuests) { quest ->
                        Text(quest.title, color = primaryColor, fontWeight = FontWeight.Bold)
                        quest.subQuests.filter { !it.isCompleted }.forEach { sub ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSubTaskToggle(sub.id) }) {
                                Checkbox(checked = selectedSubIds.contains(sub.id), onCheckedChange = { onSubTaskToggle(sub.id) })
                                Text(sub.title, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("START") }
    }
}

@Composable
fun RunningState(timeRemaining: Long, onPause: () -> Unit, onAbandon: () -> Unit, primaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BATTLE IN PROGRESS", color = primaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        Text("%02d:%02d".format(timeRemaining / 60, timeRemaining % 60), color = Color.White, fontSize = 60.sp)
        Spacer(Modifier.height(30.dp))
        Row {
            Button(onClick = onPause, colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)) { Text("Pause") }
            Spacer(Modifier.width(16.dp))
            Button(onClick = onAbandon, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Abandon") }
        }
    }
}

@Composable
fun PausedState(allowanceRemaining: Long, onResume: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PAUSED", color = Color.Yellow, fontSize = 30.sp)
        Text("Resume in ${allowanceRemaining}s", color = Color.Red)
        Button(onClick = onResume) { Text("RESUME") }
    }
}

@Composable
fun CompletionSelectState(quests: List<Quest>, selectedSubIds: Set<String>, completedSubIds: Set<String>, onToggleComplete: (String) -> Unit, onConfirm: () -> Unit) {
    Column {
        Text("Check completed tasks:", color = Color.White)
        Box(modifier = Modifier.height(300.dp)) {
            LazyColumn {
                items(quests) { quest ->
                    quest.subQuests.filter { selectedSubIds.contains(it.id) }.forEach { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onToggleComplete(sub.id) }) {
                            Checkbox(checked = completedSubIds.contains(sub.id), onCheckedChange = { onToggleComplete(sub.id) })
                            Text(sub.title, color = Color.White, textDecoration = if(completedSubIds.contains(sub.id)) TextDecoration.LineThrough else null)
                        }
                    }
                }
            }
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("CONFIRM") }
    }
}

@Composable
fun ReportState(sessionSummary: String, onSummaryChange: (String) -> Unit, isGenerating: Boolean, onGenerate: () -> Unit) {
    Column {
        Text("Battle Report:", color = Color.White)
        OutlinedTextField(value = sessionSummary, onValueChange = onSummaryChange, label = { Text("What did you learn?") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGenerate, enabled = !isGenerating, modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp)) else Text("GENERATE QUIZ")
        }
    }
}

@Composable
fun RewardState(xp: Int, coins: Int, primaryColor: Color, onClaim: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("VICTORY!", fontSize = 30.sp, color = Color.Green)
        Text("+$xp XP", color = primaryColor, fontSize = 24.sp)
        Text("+$coins Coins", color = Color.Yellow, fontSize = 24.sp)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onClaim) { Text("CLAIM REWARDS") }
    }
}
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skill_forge.app.R
import com.skill_forge.app.ui.main.models.*

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRankDialog by remember { mutableStateOf(false) }

    // Theme Colors
    val cyberBlue = Color(0xFF00E5FF)
    val cyberPurple = Color(0xFFD500F9)
    val deepBg = Brush.verticalGradient(listOf(Color(0xFF0B0F19), Color(0xFF162238)))

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
            .background(deepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            if (viewModel.userProfile.value != null) {
                HeaderStats(viewModel.userProfile.value!!, { showRankDialog = true })
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cyberBlue)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- MAIN CONTENT ANIMATION ---
            AnimatedContent(
                targetState = viewModel.state.value,
                label = "state_transition",
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 10 } togetherWith fadeOut() + slideOutVertically { -it / 10 }
                }
            ) { state ->
                when (state) {
                    SessionState.IDLE -> IdleState(
                        activeQuests = viewModel.activeQuests.value,
                        selectedSubIds = viewModel.selectedSubQuestIds.value,
                        sliderValue = viewModel.selectedDurationMin.value,
                        onSliderChange = { viewModel.selectedDurationMin.value = it },
                        onSubTaskToggle = { viewModel.toggleSubTaskSelection(it) },
                        onStart = { viewModel.startSession(context) }
                    )
                    SessionState.RUNNING -> RunningState(
                        timeRemaining = viewModel.timeRemaining.longValue,
                        onPause = { viewModel.pauseSession() },
                        onAbandon = { viewModel.abandonSession() }
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
                        onClaim = { viewModel.resetSession() }
                    )
                }
            }
            // Bottom spacing to prevent cut-off
            Spacer(modifier = Modifier.height(80.dp))
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

// ==================== UI COMPONENTS ====================

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    gradient: Brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9)))
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFF00E5FF)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) gradient else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
                .then(if (enabled) Modifier else Modifier.alpha(0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun HeaderStats(user: UserProfile, onRankClick: () -> Unit) {
    // Glassmorphism container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
            .clickable { onRankClick() }
            .padding(vertical = 16.dp, horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(iconRes = R.drawable.ic_streak_fire, value = "${user.streakDays}", label = "Streak", color = Color(0xFFFF5722))
            VerticalDivider(color = Color.White.copy(0.1f), modifier = Modifier.height(32.dp))
            StatItem(iconRes = R.drawable.coin_display_outline, value = "${user.coins}", label = "Coins", color = Color(0xFFFFD700))
            VerticalDivider(color = Color.White.copy(0.1f), modifier = Modifier.height(32.dp))
            StatItem(iconRes = R.drawable.ic_xp, value = "${user.xp}", label = "XP", color = Color(0xFF00E5FF))
        }
    }
}

@Composable
fun StatItem(iconRes: Int, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdleState(
    activeQuests: List<Quest>,
    selectedSubIds: Set<String>,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    onSubTaskToggle: (String) -> Unit,
    onStart: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "SESSION DURATION",
            color = Color(0xFF00E5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${sliderValue.toInt()} min",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = onSliderChange,
            valueRange = 1f..120f,
            steps = 22, // 5 min increments approx
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Quest Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "ACTIVE QUESTS",
                color = Color.White.copy(0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF131B29))
                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(20.dp))
            ) {
                if (activeQuests.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No active quests", color = Color.White.copy(0.3f))
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(activeQuests) { quest ->
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Text(
                                    quest.title,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                quest.subQuests.filter { !it.isCompleted }.forEach { sub ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedSubIds.contains(sub.id)) Color(0xFF00E5FF).copy(0.1f) else Color.Transparent)
                                            .clickable { onSubTaskToggle(sub.id) }
                                            .padding(8.dp)
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                            Checkbox(
                                                checked = selectedSubIds.contains(sub.id),
                                                onCheckedChange = { onSubTaskToggle(sub.id) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF00E5FF),
                                                    uncheckedColor = Color.Gray,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            sub.title,
                                            color = Color.White.copy(0.9f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(0.05f))
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        CyberButton(
            text = "START BATTLE",
            onClick = onStart,
            icon = Icons.Rounded.PlayArrow
        )
    }
}

@Composable
fun RunningState(timeRemaining: Long, onPause: () -> Unit, onAbandon: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(40.dp))

        // Pulsing Circle Effect (Static for now, can be animated)
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(0.1f), Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(2.dp, Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "FOCUS",
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "%02d:%02d".format(timeRemaining / 60, timeRemaining % 60),
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onPause,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFFFDD835))
            ) {
                Icon(Icons.Default.Pause, null, tint = Color(0xFFFDD835))
                Spacer(Modifier.width(8.dp))
                Text("PAUSE", color = Color(0xFFFDD835), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onAbandon,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFFFF1744))
            ) {
                Icon(Icons.Default.Close, null, tint = Color(0xFFFF1744))
                Spacer(Modifier.width(8.dp))
                Text("GIVE UP", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PausedState(allowanceRemaining: Long, onResume: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
    ) {
        Icon(Icons.Default.PauseCircleFilled, null, tint = Color(0xFFFFD700), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("SESSION PAUSED", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Auto-resume in ${allowanceRemaining}s", color = Color.Gray)
        Spacer(Modifier.height(48.dp))
        CyberButton(text = "RESUME BATTLE", onClick = onResume, icon = Icons.Rounded.PlayArrow)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionSelectState(
    quests: List<Quest>,
    selectedSubIds: Set<String>,
    completedSubIds: Set<String>,
    onToggleComplete: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column {
        Text("MISSION DEBRIEF", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Mark tasks completed during this session:", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131B29))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(quests) { quest ->
                    quest.subQuests.filter { selectedSubIds.contains(it.id) }.forEach { sub ->
                        val isChecked = completedSubIds.contains(sub.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) Color(0xFF00E5FF).copy(0.15f) else Color.Black.copy(0.2f))
                                .clickable { onToggleComplete(sub.id) }
                                .padding(12.dp)
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleComplete(sub.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF00E5FF),
                                        uncheckedColor = Color.Gray
                                    )
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                sub.title,
                                color = if (isChecked) Color.White else Color.Gray,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        CyberButton(text = "CONFIRM PROGRESS", onClick = onConfirm)
    }
}

@Composable
fun ReportState(sessionSummary: String, onSummaryChange: (String) -> Unit, isGenerating: Boolean, onGenerate: () -> Unit) {
    Column {
        Text("BATTLE LOG", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("What did you learn today?", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = sessionSummary,
            onValueChange = onSummaryChange,
            placeholder = { Text("I learned about...", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.White.copy(0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF00E5FF),
                focusedContainerColor = Color(0xFF131B29),    // UPDATED: Used focusedContainerColor
                unfocusedContainerColor = Color(0xFF131B29)   // UPDATED: Used unfocusedContainerColor
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(24.dp))

        CyberButton(
            text = if (isGenerating) "GENERATING..." else "GENERATE QUIZ",
            onClick = onGenerate,
            enabled = !isGenerating,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF2979FF)))
        )
    }
}

@Composable
fun RewardState(xp: Int, coins: Int, onClaim: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("VICTORY!", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))

        // Rewards Container
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(R.drawable.ic_xp), contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(8.dp))
                Text("+$xp XP", color = Color(0xFF00E5FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(R.drawable.coin_display_outline), contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(8.dp))
                Text("+$coins", color = Color(0xFFFFD700), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(64.dp))

        CyberButton(
            text = "CLAIM REWARDS",
            onClick = onClaim,
            gradient = Brush.horizontalGradient(listOf(Color(0xFFD500F9), Color(0xFFFF4081)))
        )
    }
}

// ... Keep existing RankProgressionDialog and RankItemCard unchanged as they were already custom styled ...
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
package com.skill_forge.app.ui.main.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.launch
import java.util.UUID

// ==================== DATA MODELS ====================

enum class QuestRarity(val displayName: String, val color: Color, val icon: String, val xpReward: Int) {
    COMMON("Common", Color(0xFFB0BEC5), "⚪", 100),
    RARE("Rare", Color(0xFF00E5FF), "🔵", 250),
    EPIC("Epic", Color(0xFFD500F9), "🟣", 500),
    LEGENDARY("Legendary", Color(0xFFFFAB00), "🟠", 1000)
}

data class SubQuest(val id: String = UUID.randomUUID().toString(), val title: String = "", var isCompleted: Boolean = false) {
    constructor() : this(UUID.randomUUID().toString(), "", false)
}

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val rarity: Int = 0,
    val subQuests: List<SubQuest> = emptyList(),
    val status: Int = 0, // 0: Active, 1: Completed
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(UUID.randomUUID().toString(), "", 0, emptyList(), 0, System.currentTimeMillis())
    val rarityEnum: QuestRarity get() = QuestRarity.entries.getOrElse(rarity) { QuestRarity.COMMON }
    val progress: Float get() = if (subQuests.isEmpty()) 0f else subQuests.count { it.isCompleted }.toFloat() / subQuests.size
}

// ==================== MAIN SCREEN ====================

@Composable
fun TaskScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for Quests
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewQuestDialog by remember { mutableStateOf(false) }

    // State for AI Rewards
    var showRewardDialog by remember { mutableStateOf(false) }
    var rewardCode by remember { mutableStateOf("") }
    var rewardTitle by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // Firebase Listener
    DisposableEffect(userId) {
        if (userId.isEmpty()) { isLoading = false; return@DisposableEffect onDispose { } }
        val listener = db.collection("users").document(userId).collection("quests")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
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
                        QuestCard(quest, userId, db) { completedQuest ->
                            // CALLBACK: Trigger AI when quest completes
                            scope.launch {
                                isAiLoading = true
                                rewardTitle = completedQuest.title
                                rewardCode = GeminiService.generateQuestLoot(completedQuest.title)
                                isAiLoading = false
                                showRewardDialog = true
                            }
                        }
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

        // AI Loading Overlay
        if (isAiLoading) {
            Dialog(onDismissRequest = {}) {
                Card(colors = CardDefaults.cardColors(Color(0xFF1E1E1E))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFAB00))
                        Spacer(Modifier.height(16.dp))
                        Text("Forging Loot...", color = Color.White)
                    }
                }
            }
        }

        // Reward Dialog
        if (showRewardDialog) {
            CodeRewardDialog(rewardCode, rewardTitle) { showRewardDialog = false }
        }

        // New Quest Dialog (Simplified for brevity, logic same as before)
        if (showNewQuestDialog) {
            QuickQuestDialog(onDismiss = { showNewQuestDialog = false }, onQuestCreated = { q ->
                if(userId.isNotEmpty()) db.collection("users").document(userId).collection("quests").document(q.id).set(q)
                showNewQuestDialog = false
            })
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun QuestCard(quest: Quest, userId: String, db: FirebaseFirestore, onComplete: (Quest) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(quest.rarityEnum.color.copy(0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(quest.rarityEnum.icon, fontSize = 24.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${quest.rarityEnum.displayName} • ${quest.rarityEnum.xpReward} XP", color = quest.rarityEnum.color, fontSize = 12.sp)
                }
                IconButton(onClick = { db.collection("users").document(userId).collection("quests").document(quest.id).delete() }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray)
                }
            }

            // Progress Bar
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { quest.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = quest.rarityEnum.color, trackColor = Color.White.copy(0.1f))

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                quest.subQuests.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Checkbox(
                            checked = sub.isCompleted,
                            onCheckedChange = { isChecked ->
                                val updatedSubs = quest.subQuests.map { if (it.id == sub.id) it.copy(isCompleted = isChecked) else it }
                                val allDone = updatedSubs.all { it.isCompleted }
                                val newStatus = if (allDone) 1 else 0

                                db.collection("users").document(userId).collection("quests").document(quest.id)
                                    .update(mapOf("subQuests" to updatedSubs, "status" to newStatus))

                                // Trigger completion if status changed from Active(0) to Complete(1)
                                if (allDone && quest.status == 0) {
                                    onComplete(quest)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = quest.rarityEnum.color)
                        )
                        Text(sub.title, color = if (sub.isCompleted) Color.Gray else Color.White, textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null)
                    }
                }
            }
        }
    }
}

@Composable
fun CodeRewardDialog(code: String, title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(0xFFFFAB00))
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("QUEST COMPLETE!", color = Color(0xFFFFAB00), fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("AI Loot for: $title", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))

                Box(Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text(code, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB00)), modifier = Modifier.fillMaxWidth()) {
                    Text("Claim Reward", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickQuestDialog(onDismiss: () -> Unit, onQuestCreated: (Quest) -> Unit) {
    // (Previous implementation - omitted to save space, but logically needed here)
    // Simply copy the QuickQuestDialog from the previous response here.
    var title by remember { mutableStateOf("") }
    var subTasksText by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(Color(0xFF263238))) {
            Column(Modifier.padding(24.dp)) {
                Text("New Quest", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = subTasksText, onValueChange = { subTasksText = it }, label = { Text("Subtasks (lines)") }, minLines = 3)
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val subs = subTasksText.split("\n").filter { it.isNotBlank() }.map { SubQuest(title = it.trim()) }
                        onQuestCreated(Quest(title = title, subQuests = subs))
                    }
                }) { Text("Create") }
            }
        }
    }
}
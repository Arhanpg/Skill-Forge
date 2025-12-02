package com.example.skill_forge.ui.main.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.*

// ==================== DATA MODELS ====================

enum class QuestRarity(val displayName: String, val color: Color, val icon: String, val xpReward: Int) {
    COMMON("Common", Color(0xFFB0BEC5), "⚪", 100),
    RARE("Rare", Color(0xFF00E5FF), "🔵", 250),
    EPIC("Epic", Color(0xFFD500F9), "🟣", 500),
    LEGENDARY("Legendary", Color(0xFFFFAB00), "🟠", 1000)
}

data class SubQuest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    var isCompleted: Boolean = false
) {
    // Required for Firestore deserialization
    constructor() : this(UUID.randomUUID().toString(), "", false)
}

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val rarity: String = "COMMON",
    val subQuests: List<SubQuest> = emptyList(),
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Required for Firestore deserialization
    constructor() : this(UUID.randomUUID().toString(), "", "COMMON", emptyList(), false, System.currentTimeMillis())

    val rarityEnum: QuestRarity
        get() = try { QuestRarity.valueOf(rarity) } catch (e: Exception) { QuestRarity.COMMON }

    val progress: Float
        get() = if (subQuests.isEmpty()) 0f else subQuests.count { it.isCompleted }.toFloat() / subQuests.size
}

// ==================== MAIN SCREEN ====================

@Composable
fun TaskScreen() {
    // FIX: Connect explicitly to "skillforge" database
    val db = remember { FirebaseFirestore.getInstance("skillforge") }
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewQuestDialog by remember { mutableStateOf(false) }
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Real-time listener: Faster and updates instantly
    DisposableEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            onDispose { }
        } else {
            val listener: ListenerRegistration = db.collection("users")
                .document(userId)
                .collection("quests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("TaskScreen", "Listen failed.", e)
                        isLoading = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        quests = snapshot.documents.mapNotNull { it.toObject(Quest::class.java) }
                        isLoading = false
                    }
                }
            onDispose { listener.remove() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚔️", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("QUEST BOARD", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(0.3f), CircleShape)
                    .padding(4.dp)
            ) {
                listOf("Active", "Completed").forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFF00BCD4) else Color.Transparent)
                            .clickable { selectedTab = index }
                    ) {
                        Text(title, color = if (selected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            } else {
                val filteredQuests = quests.filter { if (selectedTab == 0) !it.isCompleted else it.isCompleted }

                if (filteredQuests.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📜", fontSize = 64.sp)
                            Text("No quests found", color = Color.Gray, fontSize = 18.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredQuests, key = { it.id }) { quest ->
                            QuestItem(quest, db, userId)
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showNewQuestDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF00BCD4),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, null)
        }

        if (showNewQuestDialog) {
            NewQuestDialog(onDismiss = { showNewQuestDialog = false }) { title, rarity, subtasks ->
                if (userId.isNotEmpty()) {
                    val newQuest = Quest(
                        title = title,
                        rarity = rarity.name,
                        subQuests = subtasks.map { SubQuest(title = it) }
                    )
                    db.collection("users").document(userId).collection("quests")
                        .document(newQuest.id).set(newQuest)
                }
                showNewQuestDialog = false
            }
        }
    }
}

@Composable
fun QuestItem(quest: Quest, db: FirebaseFirestore, userId: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(40.dp)
                    .background(quest.rarityEnum.color.copy(0.2f), CircleShape)
                    .border(1.dp, quest.rarityEnum.color, CircleShape), contentAlignment = Alignment.Center
                ) {
                    Text(quest.rarityEnum.icon, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${quest.rarityEnum.displayName} • ${quest.rarityEnum.xpReward} XP", color = quest.rarityEnum.color, fontSize = 12.sp)
                }
                IconButton(onClick = {
                    db.collection("users").document(userId).collection("quests").document(quest.id).delete()
                }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { quest.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = quest.rarityEnum.color,
                trackColor = Color.White.copy(0.1f)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (quest.subQuests.isEmpty()) Text("No sub-tasks", color = Color.Gray, fontSize = 12.sp)
                    else {
                        quest.subQuests.forEach { sub ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Checkbox(
                                    checked = sub.isCompleted,
                                    onCheckedChange = { checked ->
                                        val newSubs = quest.subQuests.map { if (it.id == sub.id) it.copy(isCompleted = checked) else it }
                                        val isAllDone = newSubs.all { it.isCompleted }
                                        db.collection("users").document(userId).collection("quests").document(quest.id)
                                            .update(mapOf("subQuests" to newSubs, "isCompleted" to isAllDone))
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = quest.rarityEnum.color)
                                )
                                Text(sub.title, color = if(sub.isCompleted) Color.Gray else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewQuestDialog(onDismiss: () -> Unit, onConfirm: (String, QuestRarity, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf(QuestRarity.COMMON) }
    var subtasks by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("New Quest", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Rarity", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    QuestRarity.entries.forEach { rarity ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (selectedRarity == rarity) rarity.color else Color.Transparent, CircleShape)
                                .border(1.dp, rarity.color, CircleShape)
                                .clickable { selectedRarity = rarity },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rarity.icon)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = subtasks, onValueChange = { subtasks = it },
                    label = { Text("Subtasks (one per line)") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if(title.isNotBlank()) onConfirm(title, selectedRarity, subtasks.split("\n").filter { it.isNotBlank() })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                ) {
                    Text("CREATE QUEST")
                }
            }
        }
    }
}
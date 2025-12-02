package com.example.skill_forge.ui.main.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.UUID

// ==================== SHARED DATA MODELS ====================
// These are the ONLY definitions. HomeScreen will use these.

enum class QuestRarity(
    val displayName: String,
    val color: Color,
    val icon: String,
    val xpReward: Int
) {
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
    // No-argument constructor for Firestore deserialization
    constructor() : this(UUID.randomUUID().toString(), "", false)
}

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val rarity: Int = 0, // 0: Common, 1: Rare, etc.
    val subQuests: List<SubQuest> = emptyList(),
    val status: Int = 0, // 0: Active, 1: Completed
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this(UUID.randomUUID().toString(), "", 0, emptyList(), 0, System.currentTimeMillis())

    // Helpers for UI
    val rarityEnum: QuestRarity
        get() = QuestRarity.entries.getOrElse(rarity) { QuestRarity.COMMON }

    val progress: Float
        get() = if (subQuests.isEmpty()) 0f else subQuests.count { it.isCompleted }.toFloat() / subQuests.size
}

// ==================== TASKS SCREEN UI ====================

@Composable
fun TaskScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewQuestDialog by remember { mutableStateOf(false) }
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

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

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            QuestBoardHeader()
            QuestTabRow(selectedTab, onTabSelected = { selectedTab = it })
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else {
                // Filter: Tab 0 = Active (status 0), Tab 1 = Completed (status 1)
                val filteredQuests = quests.filter {
                    when (selectedTab) {
                        0 -> it.status == 0
                        1 -> it.status == 1
                        else -> true
                    }
                }
                OptimizedQuestList(filteredQuests, userId, db)
            }
        }

        FloatingActionButton(
            onClick = { showNewQuestDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF00E5FF),
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, "Add Quest")
        }

        if (showNewQuestDialog) {
            QuickQuestDialog(
                onDismiss = { showNewQuestDialog = false },
                onQuestCreated = { newQuest ->
                    if (userId.isNotEmpty()) {
                        db.collection("users").document(userId).collection("quests")
                            .document(newQuest.id).set(newQuest)
                    }
                    showNewQuestDialog = false
                }
            )
        }
    }
}

// ... (Keep existing smaller components like QuestBoardHeader, QuestTabRow from your file)
// Make sure to define these below if they aren't already there.

@Composable
fun QuestBoardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QUEST BOARD",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun QuestTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Active Quests", "Completed Log")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val bgColor = if (isSelected) Color(0xFF00E5FF) else Color.Transparent
            val textColor = if (isSelected) Color.Black else Color.Gray

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun OptimizedQuestList(quests: List<Quest>, userId: String, db: FirebaseFirestore) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📜", fontSize = 60.sp)
                Text("No quests found.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = quests, key = { it.id }) { quest ->
                QuestCard(quest, userId, db)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuestCard(quest: Quest, userId: String, db: FirebaseFirestore) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(quest.rarityEnum.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(quest.rarityEnum.icon, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${quest.rarityEnum.displayName} • ${quest.rarityEnum.xpReward} XP",
                        color = quest.rarityEnum.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                if (quest.subQuests.isEmpty()) {
                    Text("No sub-tasks.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    quest.subQuests.forEach { subQuest ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = subQuest.isCompleted,
                                onCheckedChange = { isChecked ->
                                    val updatedSubQuests = quest.subQuests.map {
                                        if (it.id == subQuest.id) it.copy(isCompleted = isChecked) else it
                                    }
                                    val allDone = updatedSubQuests.all { it.isCompleted }
                                    val newStatus = if (allDone) 1 else 0

                                    db.collection("users").document(userId).collection("quests")
                                        .document(quest.id)
                                        .update(mapOf("subQuests" to updatedSubQuests, "status" to newStatus))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = quest.rarityEnum.color)
                            )
                            Text(
                                text = subQuest.title,
                                color = if (subQuest.isCompleted) Color.Gray else Color.White,
                                textDecoration = if (subQuest.isCompleted) TextDecoration.LineThrough else null,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickQuestDialog(onDismiss: () -> Unit, onQuestCreated: (Quest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableIntStateOf(0) }
    var subTasksText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("New Quest", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Rarity", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    QuestRarity.entries.forEachIndexed { index, rarity ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selectedRarity == index) rarity.color else Color.Transparent,
                                    CircleShape
                                )
                                .border(1.dp, rarity.color, CircleShape)
                                .clickable { selectedRarity = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rarity.icon)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = subTasksText,
                    onValueChange = { subTasksText = it },
                    label = { Text("Subtasks (one per line)") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val subQuests = subTasksText.split("\n")
                            .filter { it.isNotBlank() }
                            .map { SubQuest(title = it.trim()) }

                        val quest = Quest(
                            title = title,
                            rarity = selectedRarity,
                            subQuests = subQuests
                        )
                        onQuestCreated(quest)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Create Quest", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
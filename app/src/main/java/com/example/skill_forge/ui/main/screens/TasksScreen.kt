package com.example.skill_forge.ui.main.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

// ==================== DATA MODELS ====================

enum class QuestRarity(
    val displayName: String,
    val color: Color,
    val icon: String,
    val xpReward: Int,
    val oreReward: String
) {
    COMMON("Common", Color(0xFFB0BEC5), "⚪", 100, "Bronze Ore"),
    RARE("Rare", Color(0xFF42A5F5), "🔵", 250, "Iron Ore"),
    EPIC("Epic", Color(0xFFAB47BC), "🟣", 500, "Gold Ore"),
    LEGENDARY("Legendary", Color(0xFFFF9800), "🟠", 1000, "Diamond Ore")
}

enum class QuestStatus {
    ACTIVE, COMPLETED, PENDING
}

data class SubQuest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    var isCompleted: Boolean = false
) {
    constructor() : this("", "", false)
}

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val rarity: QuestRarity = QuestRarity.COMMON,
    val subQuests: MutableList<SubQuest> = mutableListOf(),
    var status: QuestStatus = QuestStatus.ACTIVE,
    val difficulty: Float = 0.5f,
    val estimatedTime: Int = 60,
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
    val createdAt: Long = System.currentTimeMillis(),
    val template: QuestTemplate = QuestTemplate.CUSTOM
) {
    constructor() : this("", "", "")

    val progress: Float
        get() = if (subQuests.isEmpty()) 0f
        else subQuests.count { it.isCompleted }.toFloat() / subQuests.size.toFloat()

    val xpReward: Int
        get() = (rarity.xpReward * (1 + difficulty)).toInt()

    val daysRemaining: Int
        get() = ((dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
}

enum class QuestTemplate(val displayName: String, val icon: String) {
    STUDY("Study Session", "📖"),
    CODING("Coding Sprint", "💻"),
    CREATIVE("Creative Work", "🎨"),
    PRACTICE("Skill Practice", "🏋️"),
    RESEARCH("Research Task", "📝"),
    CUSTOM("Custom Quest", "⚡")
}

// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableStateOf(0) }
    var showNewQuestDialog by remember { mutableStateOf(false) }
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    val tabs = listOf("Active", "Long-term", "Completed")

    // Load quests from Firestore
    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            loadQuestsFromFirestore(db, userId) { loadedQuests ->
                quests = loadedQuests
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            QuestBoardHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            QuestTabRow(
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quest List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            } else {
                val filteredQuests = when (selectedTab) {
                    0 -> quests.filter { it.status == QuestStatus.ACTIVE && it.subQuests.size <= 5 }
                    1 -> quests.filter { it.status == QuestStatus.ACTIVE && it.subQuests.size > 5 }
                    2 -> quests.filter { it.status == QuestStatus.COMPLETED }
                    else -> emptyList()
                }

                QuestList(
                    quests = filteredQuests,
                    onQuestUpdated = { updatedQuest ->
                        scope.launch {
                            if (userId.isNotEmpty()) {
                                updateQuestInFirestore(db, userId, updatedQuest)
                            }
                            quests = quests.map { if (it.id == updatedQuest.id) updatedQuest else it }
                        }
                    },
                    onQuestDeleted = { questId ->
                        scope.launch {
                            if (userId.isNotEmpty()) {
                                deleteQuestFromFirestore(db, userId, questId)
                            }
                            quests = quests.filter { it.id != questId }
                        }
                    }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showNewQuestDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF00BCD4),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Quest")
        }

        // New Quest Dialog
        if (showNewQuestDialog) {
            NewQuestDialog(
                onDismiss = { showNewQuestDialog = false },
                onQuestCreated = { newQuest ->
                    scope.launch {
                        if (userId.isNotEmpty()) {
                            saveQuestToFirestore(db, userId, newQuest)
                        }
                        quests = quests + newQuest
                        showNewQuestDialog = false
                    }
                }
            )
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun QuestBoardHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "🗂️",
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "QUEST BOARD",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun QuestTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                QuestTab(
                    text = tab,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuestTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) Color(0xFF00BCD4) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun QuestList(
    quests: List<Quest>,
    onQuestUpdated: (Quest) -> Unit,
    onQuestDeleted: (String) -> Unit
) {
    if (quests.isEmpty()) {
        EmptyQuestState()
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(quests, key = { it.id }) { quest ->
                QuestCard(
                    quest = quest,
                    onQuestUpdated = onQuestUpdated,
                    onQuestDeleted = onQuestDeleted
                )
            }
        }
    }
}

@Composable
fun EmptyQuestState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🗺️",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Quests Available",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to create your first quest!",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestCard(
    quest: Quest,
    onQuestUpdated: (Quest) -> Unit,
    onQuestDeleted: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { _, _ -> }
                )
            }
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { isDragging = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, quest.rarity.color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = quest.template.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = quest.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${quest.rarity.icon} ${quest.rarity.displayName}",
                            fontSize = 12.sp,
                            color = quest.rarity.color
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5555),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sub-quests: ${quest.subQuests.count { it.isCompleted }}/${quest.subQuests.size} ✓",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(quest.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { quest.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = quest.rarity.color,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
            }

            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Sub-quests
                    quest.subQuests.forEach { subQuest ->
                        SubQuestItem(
                            subQuest = subQuest,
                            onToggle = {
                                subQuest.isCompleted = !subQuest.isCompleted
                                onQuestUpdated(quest)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rewards & Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoChip(
                            icon = "🏆",
                            text = "${quest.xpReward} XP, ${quest.rarity.oreReward}"
                        )
                        InfoChip(
                            icon = "⏰",
                            text = "${quest.daysRemaining}d left"
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Quest?") },
            text = { Text("Are you sure you want to delete '${quest.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuestDeleted(quest.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5555))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubQuestItem(
    subQuest: SubQuest,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subQuest.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subQuest.title,
            fontSize = 14.sp,
            color = if (subQuest.isCompleted) Color.Gray else Color.White,
            textDecoration = if (subQuest.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
    }
}

@Composable
fun InfoChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// ==================== NEW QUEST DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewQuestDialog(
    onDismiss: () -> Unit,
    onQuestCreated: (Quest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf(QuestRarity.COMMON) }
    var selectedTemplate by remember { mutableStateOf(QuestTemplate.CUSTOM) }
    var difficulty by remember { mutableStateOf(0.5f) }
    var estimatedTime by remember { mutableStateOf(60) }
    var subQuestInputs by remember { mutableStateOf(listOf("", "", "")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E3A47)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "🆕 CREATE NEW QUEST",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF00BCD4),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quest Template", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuestTemplate.entries.take(3).forEach { template ->
                        TemplateChip(
                            template = template,
                            selected = selectedTemplate == template,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quest Rarity", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuestRarity.entries.forEach { rarity ->
                        RarityChip(
                            rarity = rarity,
                            selected = selectedRarity == rarity,
                            onClick = { selectedRarity = rarity }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Difficulty", fontSize = 14.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Easy", fontSize = 12.sp, color = Color.Gray)
                    Text("Hard", fontSize = 12.sp, color = Color.Gray)
                }
                Slider(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00BCD4),
                        activeTrackColor = Color(0xFF00BCD4)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Estimated Time (minutes)", fontSize = 14.sp, color = Color.Gray)
                OutlinedTextField(
                    value = estimatedTime.toString(),
                    onValueChange = { estimatedTime = it.toIntOrNull() ?: 60 },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sub-quests (Optional)", fontSize = 14.sp, color = Color.Gray)
                subQuestInputs.forEachIndexed { index, value ->
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            subQuestInputs = subQuestInputs.toMutableList().apply {
                                set(index, newValue)
                            }
                        },
                        placeholder = { Text("Sub-quest ${index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val quest = Quest(
                                title = title,
                                rarity = selectedRarity,
                                template = selectedTemplate,
                                difficulty = difficulty,
                                estimatedTime = estimatedTime,
                                subQuests = subQuestInputs
                                    .filter { it.isNotBlank() }
                                    .map { SubQuest(title = it) }
                                    .toMutableList()
                            )
                            onQuestCreated(quest)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        ),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateChip(
    template: QuestTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = template.icon, fontSize = 16.sp)
        }
    }
}

@Composable
fun RarityChip(
    rarity: QuestRarity,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) rarity.color else Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, rarity.color)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rarity.icon,
                fontSize = 20.sp
            )
        }
    }
}

// ==================== FIRESTORE OPERATIONS ====================

suspend fun loadQuestsFromFirestore(
    db: FirebaseFirestore,
    userId: String,
    onLoaded: (List<Quest>) -> Unit
) {
    try {
        val snapshot = db.collection("users")
            .document(userId)
            .collection("quests")
            .get()
            .await()

        val quests = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Quest::class.java)
        }
        onLoaded(quests)
    } catch (e: Exception) {
        onLoaded(emptyList())
    }
}

suspend fun saveQuestToFirestore(
    db: FirebaseFirestore,
    userId: String,
    quest: Quest
) {
    try {
        db.collection("users")
            .document(userId)
            .collection("quests")
            .document(quest.id)
            .set(quest)
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

suspend fun updateQuestInFirestore(
    db: FirebaseFirestore,
    userId: String,
    quest: Quest
) {
    try {
        db.collection("users")
            .document(userId)
            .collection("quests")
            .document(quest.id)
            .set(quest)
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

suspend fun deleteQuestFromFirestore(
    db: FirebaseFirestore,
    userId: String,
    questId: String
) {
    try {
        db.collection("users")
            .document(userId)
            .collection("quests")
            .document(questId)
            .delete()
            .await()
    } catch (e: Exception) {
        // Handle error
    }
}

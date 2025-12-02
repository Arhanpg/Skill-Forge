package com.example.skill_forge.ui.main.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skill_forge.R
import com.example.skill_forge.ui.main.models.AdMobHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// --- Data Models for UI ---
data class CoinPack(
    val amount: Int,
    val price: String,
    val isBestValue: Boolean = false,
    val isAd: Boolean = false
)

@Composable
fun StoreScreen() {
    val context = LocalContext.current

    // Initialize AdMob Helper
    val adMobHelper = remember { AdMobHelper(context) }

    // Firebase Init
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    // Persistent Balance State
    // We default to 0 (or a loading state) until Firestore returns the real value
    var balance by remember { mutableIntStateOf(500) }

    // Sync Balance with Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            val docRef = db.collection("users").document(userId)
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    // Get 'coins' field, defaulting to 500 if it doesn't exist yet
                    val remoteBalance = snapshot.getLong("coins")?.toInt()
                    if (remoteBalance != null) {
                        balance = remoteBalance
                    } else {
                        // Initialize field if missing
                        docRef.update("coins", 500)
                    }
                }
            }
        }
    }

    // Theme Colors
    val darkBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- HEADER ---
            item {
                StoreHeader(balance = balance)
            }

            // --- SECTION: COIN SHOP ---
            item {
                SectionTitle(title = "Top Up EduCoins", icon = Icons.Default.Add)
                CoinShopRow(
                    onAdClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            if (adMobHelper.isAdReady()) {
                                adMobHelper.showRewardedAd(activity) { rewardAmount ->
                                    // SUCCESS: Ad watched completely
                                    // Calculate new balance
                                    val newBalance = balance + rewardAmount

                                    // Update Firestore (Listener will update UI)
                                    if (userId != null) {
                                        db.collection("users").document(userId)
                                            .update("coins", newBalance)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Earned $rewardAmount coins!", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to save coins", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        // Fallback for guest/offline (local only)
                                        balance += rewardAmount
                                        Toast.makeText(context, "Earned $rewardAmount coins! (Local)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Ad loading... try again in a moment", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onPurchaseClick = { pack ->
                        Toast.makeText(context, "Purchase ${pack.price}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // --- SECTION: POWER UPS ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = "Power Ups", icon = Icons.Default.Bolt)

                UtilityCard(
                    title = "Streak Freezer",
                    description = "Protect your streak for one day!",
                    price = "500 Coins",
                    iconRes = R.drawable.ic_streak_freeze,
                    colorTheme = Color(0xFF4FC3F7)
                )
            }

            // --- SECTION: VOUCHERS ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = "Vouchers", icon = Icons.Default.Star)
                AmazonVoucherCard()
            }
        }
    }
}

@Composable
fun StoreHeader(balance: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp)
            .background(Color(0xFF0F2027))
            .padding(24.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Skill Store",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Upgrade your arsenal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Cyan.copy(alpha = 0.7f)
                )
            }

            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_coin),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$balance",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CoinShopRow(
    onAdClick: () -> Unit,
    onPurchaseClick: (CoinPack) -> Unit
) {
    val packs = listOf(
        CoinPack(10, "Watch AD", isAd = true),
        CoinPack(200, "₹49"),
        CoinPack(500, "₹99", isBestValue = true),
        CoinPack(1000, "₹189")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(packs) { pack ->
            CoinCard(
                pack = pack,
                onClick = {
                    if (pack.isAd) onAdClick() else onPurchaseClick(pack)
                }
            )
        }
    }
}

@Composable
fun CoinCard(pack: CoinPack, onClick: () -> Unit) {
    val cardBackground = if (pack.isBestValue) {
        Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238)))
    }

    val textColor = if (pack.isBestValue) Color.Black else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(140.dp)
            .height(190.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(cardBackground)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Text("COINS", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                // Main Icon
                Image(painterResource(id = R.drawable.ic_coin), null, modifier = Modifier.size(64.dp))

                // Amount
                Text("${pack.amount}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)

                // Price
                Text(pack.price, fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
            }
            if (pack.isBestValue) {
                Surface(color = Color.Red, shape = RoundedCornerShape(bottomStart = 8.dp), modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("HOT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun UtilityCard(title: String, description: String, price: String, iconRes: Int, colorTheme: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(colorTheme.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(id = iconRes), null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
            }
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = colorTheme), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text(price, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AmazonVoucherCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Amazon Voucher", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Get ₹500 off on your next purchase", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("5000 Coins", color = Color(0xFFE65100), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Image(painterResource(id = R.drawable.ic_amazon), "Amazon", modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}
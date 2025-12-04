package com.skill_forge.app.ui.main.screens

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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
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
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.skill_forge.app.R
import com.skill_forge.app.ui.main.models.AdMobHelper
import kotlinx.coroutines.launch

// --- Data Models for UI ---
data class CoinPack(
    val amount: Int,
    val title: String,
    val productId: String,
    val price: String,
    val isBestValue: Boolean = false,
    val isAd: Boolean = false,
    val productDetails: ProductDetails? = null
)

@Composable
fun StoreScreen() {
    val context = LocalContext.current
    // Initialize AdMob Helper
    val adMobHelper = remember { AdMobHelper(context) }

    // Firebase Init
    val auth = remember { FirebaseAuth.getInstance() }

    // CRITICAL FIX: Use the specific database instance name "skillforge"
    val db = remember { FirebaseFirestore.getInstance("skillforge") }

    val userId = auth.currentUser?.uid

    // Persistent State
    var balance by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }

    // Product List State
    val availablePacks = remember { mutableStateListOf<CoinPack>() }

    // Define your Product IDs from Google Play Console here
    val productIds = mapOf(
        "coins_200" to 200,
        "coins_500" to 500,
        "coins_1000" to 1000
    )

    // Function to Add Coins to Firestore
    fun addCoinsToUser(amount: Int) {
        if (userId != null) {
            val updateData = hashMapOf<String, Any>(
                "coins" to FieldValue.increment(amount.toLong())
            )
            db.collection("users").document(userId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Transaction successful!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Sync Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Guest mode logic
            balance += amount
            Toast.makeText(context, "Guest: Added $amount coins", Toast.LENGTH_SHORT).show()
        }
    }

    // --- BILLING STATE ---
    var billingClientState by remember { mutableStateOf<BillingClient?>(null) }

    DisposableEffect(Unit) {
        var client: BillingClient? = null

        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // 1. Consume the item
                        val consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                        client?.consumeAsync(consumeParams) { consumeResult, _ ->
                            if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                // 2. Find which pack was bought
                                var coinsReward = 0
                                for (sku in purchase.products) {
                                    coinsReward += productIds[sku] ?: 0
                                }
                                if (coinsReward > 0) {
                                    addCoinsToUser(coinsReward)
                                }
                            }
                        }
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Toast.makeText(context, "Purchase Canceled", Toast.LENGTH_SHORT).show()
            }
        }

        client = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClientState = client

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            productIds.keys.map {
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(it)
                                    .setProductType(BillingClient.ProductType.INAPP)
                                    .build()
                            }
                        )
                        .build()

                    client?.queryProductDetailsAsync(queryProductDetailsParams) { result, productDetailsList ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            availablePacks.clear()
                            availablePacks.add(
                                CoinPack(10, "Watch AD", "ad_reward", "FREE", isAd = true)
                            )
                            productDetailsList.forEach { details ->
                                val amount = productIds[details.productId] ?: 0
                                val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
                                availablePacks.add(
                                    CoinPack(
                                        amount = amount,
                                        title = details.name,
                                        productId = details.productId,
                                        price = formattedPrice,
                                        isBestValue = amount == 500,
                                        productDetails = details
                                    )
                                )
                            }
                            availablePacks.sortBy { it.amount }
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() { }
        })

        onDispose { client?.endConnection() }
    }

    // --- REAL-TIME FIRESTORE SYNC (Coins, XP, Streak) ---
    LaunchedEffect(userId) {
        if (userId != null) {
            val docRef = db.collection("users").document(userId)
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    // Fetch all fields safely
                    balance = snapshot.getLong("coins")?.toInt() ?: 0
                    xp = snapshot.getLong("xp")?.toInt() ?: 0
                    streak = snapshot.getLong("streakDays")?.toInt() ?: 0
                }
            }
        }
    }

    // UI Content
    val darkBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(darkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Pass all stats to the header
            item {
                StoreHeader(balance = balance, xp = xp, streak = streak)
            }

            item {
                SectionTitle(title = "Top Up EduCoins", icon = Icons.Default.Add)

                if (availablePacks.size <= 1) {
                    Box(modifier = Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                }

                CoinShopRow(
                    packs = availablePacks,
                    onAdClick = {
                        val activity = context as? Activity
                        if (activity != null && adMobHelper.isAdReady()) {
                            adMobHelper.showRewardedAd(activity) { rewardAmount ->
                                val amount = if (rewardAmount > 0) rewardAmount else 10
                                addCoinsToUser(amount)
                            }
                        } else {
                            Toast.makeText(context, "Ad loading...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPurchaseClick = { pack ->
                        if (pack.productDetails != null) {
                            val client = billingClientState
                            if (client != null && client.isReady) {
                                val productDetailsParamsList = listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(pack.productDetails)
                                        .build()
                                )
                                val flowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .build()

                                client.launchBillingFlow(context as Activity, flowParams)
                            } else {
                                Toast.makeText(context, "Store not ready yet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = "Power Ups", icon = Icons.Default.Bolt)

                UtilityCard(
                    title = "Streak Freezer",
                    description = "Protect your streak for one day!",
                    price = "500 Coins",
                    iconRes = R.drawable.ic_streak_freeze, // Ensure you have this drawable or change it
                    colorTheme = Color(0xFF4FC3F7),
                    onBuyClick = {
                        if (userId != null && balance >= 500) {
                            addCoinsToUser(-500)
                            Toast.makeText(context, "Streak Freeze Purchased!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Insufficient Coins", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = "Vouchers", icon = Icons.Default.Star)
                AmazonVoucherCard()
            }
        }
    }
}

// --- UPDATED HEADER TO SHOW XP AND STREAK ---
@Composable
fun StoreHeader(balance: Int, xp: Int, streak: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp)
            .background(Color(0xFF0F2027))
            .padding(24.dp)
            .padding(top = 16.dp)
    ) {
        // Title Row
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row (XP, Streak, Coins)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // XP Chip
            StatChip(
                icon = Icons.Default.School,
                value = "$xp XP",
                color = Color(0xFFB39DDB) // Light Purple
            )

            // Streak Chip
            StatChip(
                icon = Icons.Default.LocalFireDepartment,
                value = "$streak Days",
                color = Color(0xFFFFAB91) // Light Orange
            )

            // Coins Chip
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
                        painter = painterResource(id = R.drawable.ic_coin_outline),
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
fun StatChip(icon: ImageVector, value: String, color: Color) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CoinShopRow(
    packs: List<CoinPack>,
    onAdClick: () -> Unit,
    onPurchaseClick: (CoinPack) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(packs) { pack ->
            CoinCard(
                pack = pack,
                onClick = {
                    if (pack.isAd) {
                        onAdClick()
                    } else {
                        onPurchaseClick(pack)
                    }
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
                Text("COINS", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Image(painterResource(id = R.drawable.ic_coin_outline), null, modifier = Modifier.size(64.dp))

                Text("${pack.amount}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)

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
fun UtilityCard(
    title: String,
    description: String,
    price: String,
    iconRes: Int,
    colorTheme: Color,
    onBuyClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(colorTheme.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                // Assuming you have a drawable resource. If using Vector Icons, switch this to Icon(imageVector...)
                Icon(painterResource(id = iconRes), null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
            }
            Button(
                onClick = onBuyClick,
                colors = ButtonDefaults.buttonColors(containerColor = colorTheme),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
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
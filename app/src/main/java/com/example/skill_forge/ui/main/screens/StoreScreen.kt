package com.example.skill_forge.ui.main.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skill_forge.R
import androidx.compose.foundation.BorderStroke

// --- Data Models for UI ---
data class CoinPack(
    val amount: Int,
    val price: String,
    val isBestValue: Boolean = false,
    val isAd: Boolean = false
)

@Composable
fun StoreScreen() {
    // Theme Colors
    val darkBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )
    val goldColor = Color(0xFFFFD700)

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
                StoreHeader(balance = 500)
            }

            // --- SECTION: COIN SHOP ---
            item {
                SectionTitle(title = "Top Up EduCoins", icon = Icons.Default.Add)
                CoinShopRow()
            }

            // --- SECTION: POWER UPS ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Power Ups", icon = Icons.Default.Bolt)

                // Streak Freezer
                UtilityCard(
                    title = "Streak Freezer",
                    description = "Protect your streak for one day!",
                    price = "500 Coins",
                    iconRes = R.drawable.ic_streak_freeze, // Ensure this exists
                    colorTheme = Color(0xFF29B6F6) // Ice Blue
                )
            }

            // --- SECTION: SKINS ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Hero Skins", icon = Icons.Default.Diamond)
            }

            item {
                SkinCard(
                    name = "Cyber Ninja",
                    price = "1500 Coins",
                    imageRes = R.drawable.skin_placeholder_1
                )
                Spacer(modifier = Modifier.height(16.dp))
                SkinCard(
                    name = "Void Walker",
                    price = "2500 Coins",
                    imageRes = R.drawable.skin_placeholder_2,
                    isRare = true
                )
            }

            // --- SECTION: VOUCHERS ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Rewards", icon = Icons.Default.Star)

                AmazonVoucherCard()
            }
        }
    }
}

// ----------------------------------------------------------------
// --- COMPONENT: HEADER ---
// ----------------------------------------------------------------
@Composable
fun StoreHeader(balance: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(top = 16.dp),
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

        // Balance Chip
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
                    tint = Color.Unspecified, // Use original colors of icon
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

// ----------------------------------------------------------------
// --- COMPONENT: COIN CARDS (Horizontal Scroll) ---
// ----------------------------------------------------------------
@Composable
fun CoinShopRow() {
    val packs = listOf(
        CoinPack(10, "Free", isAd = true),
        CoinPack(200, "₹49"),
        CoinPack(500, "₹99", isBestValue = true),
        CoinPack(1000, "₹189")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(packs) { pack ->
            CoinCard(pack)
        }
    }
}

@Composable
fun CoinCard(pack: CoinPack) {
    val cardBackground = if (pack.isBestValue) {
        Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))) // Gold Gradient
    } else {
        Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238))) // Dark Grey
    }

    val textColor = if (pack.isBestValue) Color.Black else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { /* Handle Purchase/Ad */ },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackground)
        ) {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (Ad or Amount)
                if (pack.isAd) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_ad), // Add this icon
                        contentDescription = "Watch Ad",
                        tint = Color.Cyan,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Coins",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_coin),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                // Amount
                Text(
                    text = "${pack.amount}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )

                // Button/Price Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (pack.isBestValue) Color.White else Color.Cyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pack.price,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }

            // Best Value Badge
            if (pack.isBestValue) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        "HOT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// --- COMPONENT: UTILITY & SKINS ---
// ----------------------------------------------------------------

@Composable
fun UtilityCard(
    title: String,
    description: String,
    price: String,
    iconRes: Int,
    colorTheme: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E272E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorTheme.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colorTheme,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
            }

            // Price Button
            Button(
                onClick = { /* Buy Freeze */ },
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
fun SkinCard(
    name: String,
    price: String,
    imageRes: Int,
    isRare: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(160.dp)
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 100f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (isRare) {
                    Text(
                        "LEGENDARY",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    price,
                    color = Color.Cyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Buy Icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color.Cyan, CircleShape)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Buy",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AmazonVoucherCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Amazon Voucher",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Get ₹500 off on your next purchase",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "5000 Coins",
                    color = Color(0xFFE65100), // Amazon Orange-ish
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            // Placeholder for Amazon Logo
            Image(
                painter = painterResource(id = R.drawable.ic_amazon),
                contentDescription = "Amazon",
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
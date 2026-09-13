package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.Amber400
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// =============================================================================
// REUSABLE PAYWALL PLAN ITEM (UPI & POINTS)
// =============================================================================
@Composable
fun PaywallPlanItem(
    title: String,
    duration: String,
    price: String,
    pointsText: String? = null,
    isPopular: Boolean = false,
    badge: String? = null,
    isActivating: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPopular) Slate850 else Slate950,
        border = BorderStroke(
            if (isPopular) 1.5.dp else 1.dp,
            if (isPopular) Emerald500.copy(alpha = 0.8f) else Slate800
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isActivating, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!badge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPopular) EmeraldGlow else Color(0x3322D3EE)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPopular) Emerald400 else Cyan400,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )

            Text(
                text = price,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPopular) Emerald400 else Slate50
            )

            if (!pointsText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberGlow,
                    border = BorderStroke(0.5.dp, Amber400.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = pointsText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber400,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = duration,
                fontSize = 10.sp,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPopular) Emerald500 else Slate800,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = if (isActivating) "..." else "Select",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPopular) Color.White else Cyan400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }
    }
}

// =============================================================================
// TAB 1: PROFILE & EARNINGS SCREEN
// =============================================================================
@Composable
fun ProfileEarningsTabContent(
    currentUser: FirebaseUser?,
    androidId: String,
    rideLogs: List<RideLogItem>,
    acceptedLogs: List<RideLogItem>,
    totalEarnings: Float,
    totalPickupKm: Float,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    subState: SubState? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val totalOrdersAccepted = acceptedLogs.size
    val totalOrders = rideLogs.size
    val avgFare = if (totalOrdersAccepted > 0) (totalEarnings / totalOrdersAccepted).toInt() else 0
    val acceptanceRate = if (totalOrders > 0) ((totalOrdersAccepted.toFloat() / totalOrders.toFloat()) * 100).toInt() else 0

    // Live subscription & Reward points balance tracking
    val liveSubState by remember(currentUser?.uid, androidId) {
        SubscriptionManager.observeSubscription(context, androidId, currentUser?.uid)
    }.collectAsStateWithLifecycle(initialValue = subState ?: SubState.Loading)

    val currentPoints = liveSubState.points

    // Plan payment selection & verification state
    var selectedPlanForPayment by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var isVerifyingPayment by remember { mutableStateOf(false) }

    // Plan Choice Dialog state: UPI vs Points
    data class PlanOption(
        val planKey: String,
        val title: String,
        val duration: String,
        val price: Int,
        val pointsCost: Int,
        val badge: String? = null
    )

    val availablePlans = remember {
        listOf(
            PlanOption("daily", "Daily", "24 Hours", 9, SubscriptionManager.POINTS_DAILY_PASS, null),
            PlanOption("weekly", "Weekly", "7 Days", 49, SubscriptionManager.POINTS_WEEKLY_PASS, "POPULAR"),
            PlanOption("monthly", "Monthly", "30 Days", 179, SubscriptionManager.POINTS_MONTHLY_PASS, "SAVE 35%")
        )
    }

    var selectedPlanForChoice by remember { mutableStateOf<PlanOption?>(null) }
    var isRedeemingPoints by remember { mutableStateOf(false) }

    // Referral Code Input & Redemption Dialog state
    var showReferralDialog by remember { mutableStateOf(false) }
    var referralCodeInput by remember { mutableStateOf("") }
    var isApplyingReferral by remember { mutableStateOf(false) }

    // Choose Payment Method (UPI vs Redeem Points) Dialog
    if (selectedPlanForChoice != null) {
        val plan = selectedPlanForChoice!!
        AlertDialog(
            onDismissRequest = {
                if (!isRedeemingPoints) selectedPlanForChoice = null
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Activate ${plan.title} Pass",
                        fontWeight = FontWeight.Bold,
                        color = Slate50,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Select how you'd like to activate your ${plan.duration} pass:",
                        fontSize = 13.sp,
                        color = Slate300
                    )

                    // Current Points indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Points Balance:",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentPoints",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber400
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "🪙", fontSize = 13.sp)
                            }
                        }
                    }

                    // Choice 1: Pay via UPI
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isRedeemingPoints) {
                                val p = selectedPlanForChoice
                                selectedPlanForChoice = null
                                if (p != null) {
                                    selectedPlanForPayment = Pair(p.planKey, p.price)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGlow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = Emerald400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pay via UPI",
                                        fontWeight = FontWeight.Bold,
                                        color = Slate50,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Instant QR / UTR verification",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }
                            Text(
                                text = "₹${plan.price}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald400
                            )
                        }
                    }

                    // Choice 2: Redeem Points
                    val hasEnoughPoints = currentPoints >= plan.pointsCost
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasEnoughPoints) Slate850 else Slate950,
                        border = BorderStroke(
                            1.dp,
                            if (hasEnoughPoints) Amber400.copy(alpha = 0.5f) else Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isRedeemingPoints) {
                                if (!hasEnoughPoints) {
                                    Toast.makeText(
                                        context,
                                        "Not enough points. Share the app to earn more!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@clickable
                                }
                                val uid = currentUser?.uid
                                if (uid.isNullOrBlank()) {
                                    Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                                    onSignIn()
                                    return@clickable
                                }

                                coroutineScope.launch {
                                    isRedeemingPoints = true
                                    val res = SubscriptionManager.redeemPointsForPass(
                                        userId = uid,
                                        androidId = androidId,
                                        plan = plan.planKey,
                                        requiredPoints = plan.pointsCost,
                                        context = context
                                    )
                                    isRedeemingPoints = false

                                    if (res.isSuccess) {
                                        val prefs = (context as? MainActivity)?.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
                                        val durationMs = when (plan.planKey.lowercase()) {
                                            "weekly" -> 7 * 24 * 3600 * 1000L
                                            "monthly" -> 30 * 24 * 3600 * 1000L
                                            else -> 24 * 3600 * 1000L
                                        }
                                        val expiry = System.currentTimeMillis() + durationMs
                                        prefs?.edit()
                                            ?.putBoolean(KEY_PREMIUM, true)
                                            ?.putLong(KEY_SUBSCRIPTION_EXPIRY, expiry)
                                            ?.putString("activePlan", plan.title)
                                            ?.apply()

                                        (context as? MainActivity)?.updateUiForPremiumStatus()
                                        (context as? MainActivity)?.loadSavedSettings()

                                        Toast.makeText(
                                            context,
                                            "🎉 ${plan.title} Pass activated! Deducted ${plan.pointsCost} 🪙",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        selectedPlanForChoice = null
                                    } else {
                                        val err = res.exceptionOrNull()?.message ?: "Redemption failed"
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberGlow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🪙", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Redeem Points",
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasEnoughPoints) Slate50 else Slate400,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (hasEnoughPoints) "Free pass with points" else "Need ${plan.pointsCost - currentPoints} more points",
                                        fontSize = 11.sp,
                                        color = if (hasEnoughPoints) Amber400 else Slate500
                                    )
                                }
                            }

                            if (isRedeemingPoints) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Amber400,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "${plan.pointsCost} 🪙",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (hasEnoughPoints) Amber400 else Slate500
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedPlanForChoice = null },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate700),
                    enabled = !isRedeemingPoints
                ) {
                    Text("Cancel", color = Slate300)
                }
            }
        )
    }

    // UPI Payment & UTR Verification Dialog
    if (selectedPlanForPayment != null) {
        val (planKey, price) = selectedPlanForPayment!!
        PaymentDialog(
            planName = planKey,
            price = price,
            isVerifying = isVerifyingPayment,
            onDismiss = {
                if (!isVerifyingPayment) selectedPlanForPayment = null
            },
            onVerify = { utr ->
                val uid = currentUser?.uid
                if (uid.isNullOrBlank()) {
                    Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                    onSignIn()
                    return@PaymentDialog
                }
                val planDays = when (planKey.lowercase()) {
                    "weekly" -> 7
                    "monthly" -> 30
                    else -> 1
                }
                (context as? MainActivity)?.saveUtrToFirebase(
                    utr = utr,
                    planName = planKey.replaceFirstChar { it.uppercase() },
                    planDays = planDays,
                    price = price
                )
                coroutineScope.launch {
                    isVerifyingPayment = true
                    val res = SubscriptionManager.verifyUtrAndActivatePass(
                        userId = uid,
                        plan = planKey,
                        androidId = androidId,
                        utr = utr,
                        context = context
                    )
                    isVerifyingPayment = false
                    if (res.isSuccess) {
                        val prefs = (context as? MainActivity)?.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
                        val durationMs = when (planKey.lowercase()) {
                            "weekly" -> 7 * 24 * 3600 * 1000L
                            "monthly" -> 30 * 24 * 3600 * 1000L
                            else -> 24 * 3600 * 1000L
                        }
                        val expiry = System.currentTimeMillis() + durationMs
                        prefs?.edit()
                            ?.putBoolean(KEY_PREMIUM, true)
                            ?.putLong(KEY_SUBSCRIPTION_EXPIRY, expiry)
                            ?.putString("activePlan", planKey.replaceFirstChar { it.uppercase() })
                            ?.remove(KEY_PENDING_PLAN_NAME)
                            ?.remove(KEY_PENDING_PLAN_DAYS)
                            ?.remove(KEY_PENDING_PLAN_PRICE)
                            ?.remove(KEY_LAST_UTR)
                            ?.apply()

                        (context as? MainActivity)?.updateUiForPremiumStatus()
                        (context as? MainActivity)?.loadSavedSettings()

                        Toast.makeText(
                            context,
                            "Payment verified! ${planKey.replaceFirstChar { it.uppercase() }} Pass activated successfully.",
                            Toast.LENGTH_LONG
                        ).show()
                        selectedPlanForPayment = null
                    } else {
                        val errorMsg = res.exceptionOrNull()?.message ?: "Verification failed. Please check your UTR."
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Apply Referral Code Dialog
    if (showReferralDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isApplyingReferral) showReferralDialog = false
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎁", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enter Referral Code",
                        fontWeight = FontWeight.Bold,
                        color = Slate50,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a friend's referral code to instantly claim 50 Reward Points! Your friend will also receive 20 Points.",
                        fontSize = 13.sp,
                        color = Slate300
                    )

                    OutlinedTextField(
                        value = referralCodeInput,
                        onValueChange = { referralCodeInput = it.uppercase() },
                        label = { Text("Referral Code", color = Slate400) },
                        placeholder = { Text("e.g. RAPIDO or 6-digit code", color = Slate500) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate50,
                            unfocusedTextColor = Slate100,
                            cursorColor = Amber400
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("referral_code_input")
                    )

                    if (currentUser == null) {
                        Text(
                            text = "⚠️ Please sign in with Google to claim your referral points.",
                            fontSize = 12.sp,
                            color = Amber400
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            Toast.makeText(context, "Please sign in with Google first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (referralCodeInput.isBlank()) {
                            Toast.makeText(context, "Please enter a referral code.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            isApplyingReferral = true
                            val result = SubscriptionManager.applyReferralCode(
                                currentUserId = uid,
                                referralCodeInput = referralCodeInput,
                                context = context
                            )
                            isApplyingReferral = false
                            if (result.isSuccess) {
                                Toast.makeText(context, result.getOrNull() ?: "Points awarded successfully!", Toast.LENGTH_LONG).show()
                                showReferralDialog = false
                                referralCodeInput = ""
                            } else {
                                val err = result.exceptionOrNull()?.message ?: "Failed to apply referral code."
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isApplyingReferral,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber400,
                        contentColor = Slate950
                    ),
                    modifier = Modifier.testTag("apply_referral_submit_button")
                ) {
                    if (isApplyingReferral) {
                        CircularProgressIndicator(
                            color = Slate950,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(text = "Apply & Claim 50 🪙", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReferralDialog = false },
                    enabled = !isApplyingReferral,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_user_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentUser?.photoUrl != null) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, Emerald400, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyanGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.displayName?.take(1) ?: currentUser?.email?.take(1) ?: "C").uppercase(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Rapido Captain",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentUser?.email ?: "Google Account Connected",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                // Device ID info row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "DEVICE HARDWARE ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Text(
                                text = androidId,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate300
                            )
                        }
                        if (currentUser != null) {
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate700),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                modifier = Modifier.testTag("profile_sign_out_button")
                            ) {
                                Text("Sign Out", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = onSignIn,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Color.White),
                                modifier = Modifier.testTag("profile_sign_in_button")
                            ) {
                                Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // REWARD POINTS BALANCE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reward_points_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.5.dp, Amber400.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AmberGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🪙", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reward Points Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "50 Welcome Bonus • Redeem Free Passes",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Amber400.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$currentPoints",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Amber400
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🪙",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Points summary banner & Redeem cost hint
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "DAILY PASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Text(text = "${SubscriptionManager.POINTS_DAILY_PASS} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                        }
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Slate800))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "WEEKLY PASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Text(text = "${SubscriptionManager.POINTS_WEEKLY_PASS} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                        }
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Slate800))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MONTHLY PASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Text(text = "${SubscriptionManager.POINTS_MONTHLY_PASS} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber400)
                        }
                    }
                }

                // Refer & Earn card description requested:
                // "Share your referral link with friends. When they install the app, you both get 20 Points!"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎁", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Refer & Earn",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber400
                            )
                        }
                        Text(
                            text = "Share your referral link with friends. When they install the app, you both get 20 Points!",
                            fontSize = 12.sp,
                            color = Slate300,
                            lineHeight = 17.sp
                        )
                        if (currentUser != null) {
                            val userReferralCode = remember(currentUser.uid) {
                                SubscriptionManager.getReferralCodeForUser(currentUser.uid)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Referral Code: $userReferralCode",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate300
                                )
                                Text(
                                    text = "Tap to Copy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan400,
                                    modifier = Modifier.clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Referral Code", userReferralCode)
                                        clipboard?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Refer & Earn Button and Enter Referral Code Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val referralCode = if (currentUser != null) {
                                SubscriptionManager.getReferralCodeForUser(currentUser.uid)
                            } else ""
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Download Rapido Auto Accept & Earn Free Passes")
                                val referralLink = "https://github.com/autoacceptapp/auto-accept/releases/latest"
                                val referralText = if (referralCode.isNotBlank()) {
                                    "Download Rapido Auto Accept and get Free Premium passes! Use my referral code '$referralCode' or link: $referralLink"
                                } else {
                                    "Download Rapido Auto Accept and get Free Premium passes! Use my referral link: $referralLink"
                                }
                                putExtra(Intent.EXTRA_TEXT, referralText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Referral Link"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("refer_and_earn_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber400,
                            contentColor = Slate950
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Link",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showReferralDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enter_referral_code_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Amber400
                        ),
                        border = BorderStroke(1.dp, Amber400.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Enter Code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // UPI PASSES & POINTS REDEMPTION CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_passes_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Color(0x3306B6D4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Instant Passes & Points",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "Unlock 100-600ms speed & custom filters",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }
                }

                // 3 Plan Cards (Daily, Weekly, Monthly) with points cost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availablePlans.forEach { plan ->
                        PaywallPlanItem(
                            title = plan.title,
                            duration = plan.duration,
                            price = "₹${plan.price}",
                            pointsText = "or ${plan.pointsCost} 🪙",
                            isPopular = plan.planKey == "weekly",
                            badge = plan.badge,
                            isActivating = isVerifyingPayment || isRedeemingPoints,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please sign in with Google to activate a pass", Toast.LENGTH_SHORT).show()
                                    onSignIn()
                                } else {
                                    selectedPlanForChoice = plan
                                }
                            }
                        )
                    }
                }
            }
        }

        // 2. Total Earnings Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("total_earnings_hero_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Color(0x3310B981)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL EARNINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "₹${totalEarnings.toInt()}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate50
                )

                Text(
                    text = "Calculated from $totalOrdersAccepted auto-accepted orders recorded locally on device.",
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        }

        // 3. Quick Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accepted Rides Tile
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "ACCEPTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Text(text = "$totalOrdersAccepted", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                    Text(text = "Rides", fontSize = 11.sp, color = Slate500)
                }
            }

            // Average Fare Tile
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "AVG FARE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Text(text = "₹$avgFare", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                    Text(text = "Per ride", fontSize = 11.sp, color = Slate500)
                }
            }

            // Total Distance Tile
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "DISTANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Text(text = "${String.format(Locale.US, "%.1f", totalPickupKm)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    Text(text = "Kilometers", fontSize = 11.sp, color = Slate500)
                }
            }
        }

        // 4. Performance & Conversion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Acceptance Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                    Text(
                        text = "$acceptanceRate%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                }

                LinearProgressIndicator(
                    progress = { (acceptanceRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Emerald400,
                    trackColor = Slate800
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Evaluated: $totalOrders",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                    Text(
                        text = "Ignored: ${rideLogs.size - totalOrdersAccepted}",
                        fontSize = 12.sp,
                        color = Rose400
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB 2: ORDER HISTORY (LOGS) SCREEN
// =============================================================================
@Composable
fun OrderHistoryTabContent(
    rideLogs: List<RideLogItem>,
    acceptedLogs: List<RideLogItem>,
    ignoredLogs: List<RideLogItem>,
    totalEarnings: Float,
    totalPickupKm: Float,
    isLogsLoading: Boolean
) {
    var selectedFilterTab by rememberSaveable { mutableStateOf(1) } // 0: All, 1: Accepted, 2: Ignored

    val displayedLogs = remember(selectedFilterTab, rideLogs, acceptedLogs, ignoredLogs) {
        when (selectedFilterTab) {
            1 -> acceptedLogs.take(10)
            2 -> ignoredLogs
            else -> rideLogs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top 4 KPI Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // KPI 1: Accepted
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_accepted"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Color(0x3310B981))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "ACCEPTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${acceptedLogs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate50)
                }
            }

            // KPI 2: Ignored
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_ignored"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Color(0x33EF4444))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "IGNORED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Rose400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${ignoredLogs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate50)
                }
            }

            // KPI 3: Total KM
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_km"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Color(0x3306B6D4))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "TOTAL KM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${String.format(Locale.US, "%.1f", totalPickupKm)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate50)
                }
            }

            // KPI 4: Earnings
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_earnings"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "EARNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "₹${totalEarnings.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate50)
                }
            }
        }

        // Local Phone Storage & Simulation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("local_storage_source_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Local Phone Storage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = "Offline JSON in SharedPreferences",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }

                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export CSV Button
                    Button(
                        onClick = {
                            if (displayedLogs.isEmpty()) {
                                Toast.makeText(context, "No orders to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val res = AutoAcceptService.shareRideLogsAsCsv(context, displayedLogs)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Exporting ${displayedLogs.size} orders to CSV...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500.copy(alpha = 0.18f), contentColor = Cyan400),
                        border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_orders_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Simulate Order Button
                    Button(
                        onClick = {
                            val samplePrices = listOf(65f, 95f, 130f, 180f, 220f)
                            val sampleDists = listOf(1.2f, 2.5f, 3.8f, 4.5f, 6.0f)
                            val samplePickups = listOf("Indiranagar 100ft Rd", "MG Road Metro", "Koramangala 4th Block", "HSR Layout Sec 2", "Whitefield Main Rd")
                            val sampleDrops = listOf("Koramangala 5th Block", "Indiranagar Club", "Electronic City Phase 1", "Bellandur EcoSpace", "Outer Ring Road")
                            val randIdx = (samplePrices.indices).random()

                            AutoAcceptService.logRideLocally(
                                context = context,
                                price = samplePrices[randIdx],
                                pickupKm = sampleDists[randIdx],
                                pickupLocation = samplePickups[randIdx],
                                dropLocation = sampleDrops[randIdx],
                                status = "ACCEPTED",
                                reason = "Simulated Order",
                                sourcePackage = AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Emerald400),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("simulate_order_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Filter Tabs: All, Accepted, Ignored
        TabRow(
            selectedTabIndex = selectedFilterTab,
            containerColor = Slate900,
            contentColor = Slate100,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilterTab]),
                    color = Emerald400
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Slate800, RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedFilterTab == 0,
                onClick = { selectedFilterTab = 0 },
                text = {
                    Text(
                        text = "All (${rideLogs.size})",
                        fontWeight = if (selectedFilterTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedFilterTab == 0) Emerald400 else Slate400,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.testTag("tab_all_logs")
            )
            Tab(
                selected = selectedFilterTab == 1,
                onClick = { selectedFilterTab = 1 },
                text = {
                    Text(
                        text = "Recent Accepted",
                        fontWeight = if (selectedFilterTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedFilterTab == 1) Emerald400 else Slate400,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.testTag("tab_accepted_logs")
            )
            Tab(
                selected = selectedFilterTab == 2,
                onClick = { selectedFilterTab = 2 },
                text = {
                    Text(
                        text = "Ignored (${ignoredLogs.size})",
                        fontWeight = if (selectedFilterTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedFilterTab == 2) Rose400 else Slate400,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.testTag("tab_ignored_logs")
            )
        }

        // Logs List
        if (isLogsLoading && rideLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Emerald400)
            }
        } else if (displayedLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "No order logs found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate300
                        )
                        Text(
                            text = "Orders processed by Rapido Auto Accept will appear here in real time.",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedLogs, key = { it.id.ifBlank { "${it.createdMillis}_${it.hashCode()}" } }) { logItem ->
                    OrderLogItemCard(item = logItem)
                }
            }
        }
    }
}

@Composable
fun OrderLogItemCard(item: RideLogItem) {
    val isAccepted = item.status.equals("ACCEPTED", ignoreCase = true)
    val formattedTime = remember(item.createdMillis) {
        try {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(item.createdMillis))
        } catch (e: Exception) {
            "Recent"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ride_log_item_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (isAccepted) Color(0x3310B981) else Color(0x33EF4444)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Status badge + Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAccepted) Color(0x2610B981) else Color(0x26EF4444),
                        border = BorderStroke(1.dp, if (isAccepted) Color(0x4D10B981) else Color(0x4DEF4444))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isAccepted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isAccepted) Emerald400 else Rose400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.status.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccepted) Emerald400 else Rose400
                            )
                        }
                    }
                    if (item.reason.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.reason,
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                    }
                }

                Text(
                    text = "₹${item.price.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAccepted) Emerald400 else Slate300
                )
            }

            // Route Details
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Pickup
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Emerald400)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.pickupLocation.ifBlank { "Pickup Location" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.pickupKm > 0f) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "${item.pickupKm} km",
                                fontSize = 10.sp,
                                color = Cyan400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Drop
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Rose400)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.dropLocation.ifBlank { "Destination Drop" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate300,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Footer Row: Timestamp & Source Package
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                Text(
                    text = "Rapido Captain",
                    fontSize = 10.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// UPI PAYMENT GATEWAY & UTR VERIFICATION DIALOG
// =============================================================================
@Composable
fun PaymentDialog(
    planName: String,
    price: Int,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    isVerifying: Boolean = false
) {
    val context = LocalContext.current
    var utrInput by remember { mutableStateOf("") }
    val upiUri = "upi://pay?pa=autoaccept6122-1@okhdfcbank&pn=Auto%20Accept&am=$price&cu=INR"
    val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${Uri.encode(upiUri)}"

    Dialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .padding(vertical = 20.dp)
                .testTag("payment_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${planName.replaceFirstChar { it.uppercase() }} Pass (₹$price)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Direct UPI Instant Activation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald400
                        )
                    }

                    IconButton(
                        onClick = { if (!isVerifying) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }

                // QR Code Container
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(190.dp)
                        .padding(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "UPI Payment QR Code",
                            modifier = Modifier
                                .size(175.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Text(
                    text = "Scan QR with Google Pay, PhonePe, Paytm, or BHIM",
                    fontSize = 11.sp,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )

                // Payee Details & Copy UPI ID
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "UPI ID (TAP TO COPY)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Text(
                                text = "autoaccept6122-1@okhdfcbank",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate300
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("UPI ID", "autoaccept6122-1@okhdfcbank")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "UPI ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate700),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Cyan400
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp, color = Cyan400)
                        }
                    }
                }

                // Pay via UPI App Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No UPI app found. Please scan the QR code above.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pay_via_upi_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay via Installed UPI App", fontWeight = FontWeight.Bold)
                }

                // Step 2: UTR Input Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate850,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STEP 2: ENTER 12-DIGIT UTR / REF NUMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber400,
                            letterSpacing = 0.5.sp
                        )

                        OutlinedTextField(
                            value = utrInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(12)
                                utrInput = clean
                            },
                            placeholder = { Text("e.g. 423589123456", color = Slate500, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate50,
                                unfocusedTextColor = Slate300,
                                focusedBorderColor = Emerald400,
                                unfocusedBorderColor = Slate700,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("utr_input_field")
                        )

                        Text(
                            text = "Enter the 12-digit UTR/Ref No. from your payment receipt to instantly claim your pass.",
                            fontSize = 11.sp,
                            color = Slate400,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Verify & Activate Button
                Button(
                    onClick = {
                        if (utrInput.length < 10) {
                            Toast.makeText(context, "Please enter a valid 12-digit UTR number.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onVerify(utrInput.trim())
                    },
                    enabled = utrInput.length >= 10 && !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("verify_activate_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500,
                        contentColor = Slate950,
                        disabledContainerColor = Slate800,
                        disabledContentColor = Slate500
                    )
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Slate950,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verifying Payment...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Activate Pass", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

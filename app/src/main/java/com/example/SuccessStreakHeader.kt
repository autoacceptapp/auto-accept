package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Palette for Success Streak Gamification
private val StreakSlate950 = Color(0xFF020617)
private val StreakSlate900 = Color(0xFF0F172A)
private val StreakSlate800 = Color(0xFF1E293B)
private val StreakSlate700 = Color(0xFF334155)
private val StreakSlate500 = Color(0xFF64748B)
private val StreakSlate400 = Color(0xFF94A3B8)
private val StreakSlate300 = Color(0xFFCBD5E1)
private val StreakSlate200 = Color(0xFFE2E8F0)
private val StreakSlate50 = Color(0xFFF8FAFC)

private val StreakAmber500 = Color(0xFFF59E0B)
private val StreakAmber400 = Color(0xFFFBBF24)
private val StreakAmberGlow = Color(0x33F59E0B)

private val StreakOrange500 = Color(0xFFF97316)
private val StreakOrange400 = Color(0xFFFB923C)
private val StreakOrangeGlow = Color(0x33F97316)

private val StreakEmerald500 = Color(0xFF10B981)
private val StreakEmerald400 = Color(0xFF34D399)
private val StreakEmeraldGlow = Color(0x2610B981)

private val StreakRose500 = Color(0xFFEF4444)
private val StreakRose400 = Color(0xFFF87171)
private val StreakRoseGlow = Color(0x26EF4444)

/**
 * Compact Success Streak pill shown in the TopAppBar / Header.
 */
@Composable
fun SuccessStreakTopBarBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) StreakAmberGlow else Color(0x26334155),
        border = BorderStroke(
            1.dp,
            if (isActive) StreakAmber500.copy(alpha = 0.75f) else StreakSlate700
        ),
        modifier = modifier.testTag("topbar_streak_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isActive) "🔥" else "⚡",
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$streak Streak",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) StreakAmber400 else StreakSlate400,
                modifier = Modifier.testTag("topbar_streak_text")
            )
        }
    }
}

/**
 * Full gamified Success Streak Header Card positioned prominently at the top of Tab 0.
 * Tracks consecutive accepted rides, resetting to 0 if a ride is missed, filtered, or rejected.
 */
@Composable
fun SuccessStreakHeaderCard(
    streak: Int,
    bestStreak: Int,
    onSimulateAccepted: () -> Unit = {},
    onSimulateMissed: () -> Unit = {},
    onResetStreak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0

    // Pulsing flame animation when streak is alive
    val flameScale = remember { Animatable(1f) }
    LaunchedEffect(streak) {
        if (streak > 0) {
            flameScale.animateTo(
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            flameScale.snapTo(1f)
        }
    }

    // Determine current streak tier info
    val (tierTitle, tierColor, tierEmoji) = when {
        streak == 0 -> Triple("STANDBY", StreakSlate400, "⚡")
        streak in 1..2 -> Triple("WARMING UP", StreakOrange400, "⚡")
        streak in 3..4 -> Triple("ON FIRE", StreakAmber400, "🔥")
        streak in 5..9 -> Triple("SUPERCHARGED", StreakEmerald400, "🚀")
        streak in 10..19 -> Triple("UNSTOPPABLE", Color(0xFF38BDF8), "⚡")
        else -> Triple("LEGENDARY", Color(0xFFFFD700), "👑")
    }

    // Determine next milestone
    val (nextTarget, milestoneName) = when {
        streak < 3 -> Pair(3, "Bronze Tier (3 rides)")
        streak < 5 -> Pair(5, "Silver Tier (5 rides)")
        streak < 10 -> Pair(10, "Gold Tier (10 rides)")
        streak < 20 -> Pair(20, "Diamond Tier (20 rides)")
        else -> Pair(streak + 10, "Legendary Master (+10)")
    }
    val progress = (streak.toFloat() / nextTarget).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("success_streak_header"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StreakSlate900),
        border = BorderStroke(
            1.5.dp,
            if (isActive) StreakAmber500.copy(alpha = 0.7f) else StreakSlate800
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isActive) Color(0x38F59E0B) else Color(0x1A1E293B),
                            StreakSlate900
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // ROW 1: HEADER & STATUS BADGE
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isActive) StreakAmberGlow else Color(0x2664748B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isActive) "🔥" else "⚡",
                            fontSize = 24.sp,
                            modifier = Modifier
                                .scale(flameScale.value)
                                .testTag("streak_flame_icon")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SUCCESS STREAK",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = StreakSlate50,
                                modifier = Modifier.testTag("success_streak_title")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = tierColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "$tierEmoji $tierTitle",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tierColor,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("streak_tier_badge")
                                )
                            }
                        }

                        Text(
                            text = if (isActive) "Consecutive accepted rides without missing" else "Accept consecutive rides to start heat",
                            fontSize = 11.sp,
                            color = StreakSlate400
                        )
                    }
                }

                // Best Streak Record Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StreakSlate950,
                    border = BorderStroke(1.dp, StreakSlate800),
                    modifier = Modifier.testTag("best_streak_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(text = "🏆", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Best: $bestStreak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakAmber400,
                            modifier = Modifier.testTag("best_streak_text")
                        )
                    }
                }
            }

            // =========================================================================
            // ROW 2: MAIN COUNTER DISPLAY
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StreakSlate950,
                border = BorderStroke(
                    1.dp,
                    if (isActive) StreakAmber500.copy(alpha = 0.4f) else StreakSlate800
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = streak.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isActive) StreakAmber400 else StreakSlate500,
                            modifier = Modifier.testTag("success_streak_counter_value")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Accepted In A Row",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreakSlate50
                            )
                            Text(
                                text = if (streak == 1) "1 ride claimed" else "$streak rides claimed",
                                fontSize = 12.sp,
                                color = if (isActive) StreakOrange400 else StreakSlate500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Multiplier / Gamification Perk Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) Color(0x3310B981) else Color(0x1A64748B),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) StreakEmerald500.copy(alpha = 0.6f) else StreakSlate700
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    streak >= 10 -> "3.0x"
                                    streak >= 5 -> "2.0x"
                                    streak >= 3 -> "1.5x"
                                    streak >= 1 -> "1.2x"
                                    else -> "1.0x"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActive) StreakEmerald400 else StreakSlate400
                            )
                            Text(
                                text = "FOCUS XP",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreakSlate400
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // ROW 3: RESET ON MISS RULE CALLOUT
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isActive) Color(0x2EF59E0B) else Color(0x1AF59E0B),
                border = BorderStroke(1.dp, StreakAmber500.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("success_streak_rule_notice")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Streak Rule",
                        tint = StreakAmber400,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gamified Rule: Streak resets to 0 immediately if an incoming ride is missed, filtered, or rejected.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = StreakSlate200
                    )
                }
            }

            // =========================================================================
            // ROW 4: MILESTONE PROGRESS BAR & PIPS
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next: $milestoneName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StreakSlate300
                    )
                    Text(
                        text = "$streak / $nextTarget",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StreakAmber400
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("streak_progress_bar"),
                    color = StreakAmber400,
                    trackColor = StreakSlate800,
                    strokeCap = StrokeCap.Round
                )

                // Milestone Pips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple(3, "🥉 Bronze", streak >= 3),
                        Triple(5, "🥈 Silver", streak >= 5),
                        Triple(10, "🥇 Gold", streak >= 10),
                        Triple(20, "👑 Diamond", streak >= 20)
                    ).forEach { (milestoneVal, title, reached) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reached) StreakAmberGlow else StreakSlate950,
                            border = BorderStroke(
                                1.dp,
                                if (reached) StreakAmber500.copy(alpha = 0.6f) else StreakSlate800
                            )
                        ) {
                            Text(
                                text = "$title ($milestoneVal)",
                                fontSize = 9.sp,
                                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                                color = if (reached) StreakAmber400 else StreakSlate500,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // ROW 5: QUICK TEST ACTIONS (Gamification Simulator Controls)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simulate Accepted Ride Button
                Button(
                    onClick = onSimulateAccepted,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("streak_simulate_accept_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StreakEmerald500,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "+1 Accepted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Simulate Missed Ride (triggers reset) Button
                Button(
                    onClick = onSimulateMissed,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("streak_simulate_miss_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StreakRose500,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Miss (Reset)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Manual Reset Button
                OutlinedButton(
                    onClick = onResetStreak,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("streak_reset_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StreakSlate700),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StreakSlate300
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Streak",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

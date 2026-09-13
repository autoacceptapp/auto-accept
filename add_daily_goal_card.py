import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

card_composable = """
            // =========================================================================
            // DAILY GOAL CARD
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_goal_card")
                    .clickable { showGoalEditDialog = true },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dailyTripCount / $dailyGoal rides accepted",
                            fontSize = 13.sp,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to set your target for today",
                            fontSize = 11.sp,
                            color = Cyan400,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    val progress = if (dailyGoal > 0) (dailyTripCount.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Slate800,
                            strokeWidth = 6.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (progress >= 1f) Emerald400 else Cyan400,
                            strokeWidth = 6.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                    }
                }
            }
"""

content = content.replace("            // BATTERY OPTIMIZATION WARNING", card_composable.lstrip() + "\n            // BATTERY OPTIMIZATION WARNING")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

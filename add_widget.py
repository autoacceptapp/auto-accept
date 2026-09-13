import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add collection for daily trips to the dashboard screen state
daily_trip_collect = """
    val dailyTripCount by AutoAcceptService.dailyTripCount.collectAsStateWithLifecycle()

    val isAcceptAllActive = isMasterSwitchOn && !isDistanceFilterOn && !isPriceFilterOn && !isBlacklistFilterOn
"""
content = content.replace("    val isAcceptAllActive = isMasterSwitchOn && !isDistanceFilterOn && !isPriceFilterOn && !isBlacklistFilterOn", daily_trip_collect)

# Also add the sync in LaunchedEffect
sync_call = """
    LaunchedEffect(Unit) {
        AutoAcceptService.syncDailyTripCount(context)
        val target = (context as? Activity)?.intent?.getIntExtra("TARGET_TAB", -1) ?: -1
"""
content = content.replace("""    LaunchedEffect(Unit) {
        val target = (context as? Activity)?.intent?.getIntExtra("TARGET_TAB", -1) ?: -1""", sync_call)

# Add the widget UI just before VisualLogViewCard
widget_ui = """            // =========================================================================
            // DAILY TRIP COUNTER WIDGET
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_trip_counter_card"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x333B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Car Icon",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Daily Trip Counter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "Auto-resets at midnight",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }
                    
                    Text(
                        text = dailyTripCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // ========================================================================="""

content = content.replace("            // =========================================================================\n            VisualLogViewCard(", widget_ui + "\n            VisualLogViewCard(")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
print("Added widget")

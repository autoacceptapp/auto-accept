import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add tripHistory collection
history_collect = """
    val dailyTripCount by AutoAcceptService.dailyTripCount.collectAsStateWithLifecycle()
    val tripHistory by AutoAcceptService.tripHistory.collectAsStateWithLifecycle()
"""
content = content.replace("    val dailyTripCount by AutoAcceptService.dailyTripCount.collectAsStateWithLifecycle()", history_collect)

# Add the Chart UI right below the Daily Trip Counter
target_marker = """                    Text(
                        text = dailyTripCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // ========================================================================="""

chart_ui = """                    Text(
                        text = dailyTripCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // =========================================================================
            // 7-DAY TRIP HISTORY CHART (Native Compose Chart)
            // =========================================================================
            if (tripHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("weekly_chart_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "7-Day History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Combine history + today
                        val chartData = remember(tripHistory, dailyTripCount) {
                            val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                            val list = tripHistory.map { 
                                val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it.first)
                                val label = parsed?.let { d -> sdf.format(d) } ?: it.first
                                Pair(label, it.second) 
                            }.toMutableList()
                            list.add(Pair("Today", dailyTripCount))
                            list.takeLast(7)
                        }

                        val maxVal = (chartData.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            chartData.forEach { (label, count) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    val heightPercent = count.toFloat() / maxVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .fillMaxHeight(heightPercent)
                                            .width(28.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Emerald400, Emerald600)
                                                )
                                            )
                                    ) {
                                        if (count > 0) {
                                            Text(
                                                text = count.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = Slate400,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================================="""

content = content.replace(target_marker, chart_ui)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
print("Added Chart UI")

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

pulse_and_summary_header_code = """    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(acceptedCount) {
        if (acceptedCount > 0) {
            pulseScale.animateTo(
                targetValue = 1.02f,
                animationSpec = tween(150)
            )
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(300)
            )
        }
    }

    val sessionEarnings = remember(serviceEvents) {
        serviceEvents.filter { it.type == ServiceEventType.ORDER_ACCEPTED }
            .mapNotNull { event ->
                val match = Regex("₹(\\\\d+)").find(event.description) ?: Regex("₹(\\\\d+)").find(event.title)
                match?.groupValues?.get(1)?.toIntOrNull()
            }
            .sum()
    }

    val displayedEvents = if (isExpanded) filteredEvents else filteredEvents.take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale.value)
            .testTag("visual_log_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Session Summary Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Emerald900.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SESSION EARNINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                        Text(
                            text = "₹$sessionEarnings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate50
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ACCEPTED RIDES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                        Text(
                            text = "$acceptedCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate50
                        )
                    }
                }
            }

            // Header Row"""

# Do substitution
content = re.sub(
    r'    val displayedEvents = if \(isExpanded\) filteredEvents else filteredEvents\.take\(5\)\n\n    Card\(\n        modifier = Modifier\n            \.fillMaxWidth\(\)\n            \.testTag\("visual_log_card"\),\n        shape = RoundedCornerShape\(22\.dp\),\n        colors = CardDefaults\.cardColors\(containerColor = Slate900\),\n        border = BorderStroke\(1\.dp, Slate800\)\n    \) \{\n        Column\(\n            modifier = Modifier\.padding\(16\.dp\),\n            verticalArrangement = Arrangement\.spacedBy\(14\.dp\)\n        \) \{\n            // Header Row',
    pulse_and_summary_header_code,
    content
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

warning_code = """
            // =========================================================================
            // BATTERY OPTIMIZATION WARNING
            // =========================================================================
            if (!isBatteryOptimizationIgnored && isAccessibilityEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("battery_warning_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AF59E0B)),
                    border = BorderStroke(1.dp, Color(0x4DF59E0B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Amber500,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization Enabled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Amber400
                            )
                            Text(
                                text = "This may cause Android to aggressively kill the auto-accept service in the background. Please unrestrict battery usage.",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Slate200
                            )
                        }
                        Button(
                            onClick = { openBatteryOptimizationSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
"""

content = content.replace("            // 1. MASTER AUTO-ACCEPT SWITCH CARD", warning_code.lstrip() + "\n            // 1. MASTER AUTO-ACCEPT SWITCH CARD")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

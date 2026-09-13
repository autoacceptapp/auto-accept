import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace the TrialExpired/NoSubscription block
paywall_regex = r"(is SubState\.TrialExpired, is SubState\.NoSubscription -> \{\s*Card\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.testTag\(\"paywall_card\"\),[\s\S]*?)(\s*// Choose Payment Method \(UPI vs Redeem Points\) Dialog)"
match = re.search(paywall_regex, content)
if not match:
    print("Failed to find paywall block!")
else:
    print("Found paywall block!")

new_paywall = """is SubState.TrialExpired, is SubState.NoSubscription -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTab = 1 }
                            .testTag("paywall_card_compact"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Expired",
                                tint = Amber400,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (s is SubState.TrialExpired) "Free Trial Expired" else "Subscription Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50
                                )
                                Text(
                                    text = "Tap here to view passes",
                                    fontSize = 13.sp,
                                    color = Amber400
                                )
                            }
                        }
                    }
                }"""

content = content.replace(match.group(1), new_paywall + "\n            }\n\n")

# Remove Distance, Price, Blacklist filters
filters_regex = r"(// =========================================================================\s*// 2\. DISTANCE FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)(// =========================================================================\s*// 5\. REAL-TIME VISUAL LOG VIEW \(Events & Telemetry\))"
match2 = re.search(filters_regex, content)
if not match2:
    print("Failed to find filters block!")
else:
    print("Found filters block!")

content = content.replace(match2.group(1), "")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

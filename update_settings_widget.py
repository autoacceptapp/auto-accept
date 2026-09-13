import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add ignoredCount parameter to SettingsTabContent
content = re.sub(
    r'onUpdateAvailable: \(GitHubUpdateManager\.UpdateInfo\) -> Unit,\n\s*modifier: Modifier = Modifier',
    r'onUpdateAvailable: (GitHubUpdateManager.UpdateInfo) -> Unit,\n    ignoredCount: Int,\n    modifier: Modifier = Modifier',
    content
)

# Pass ignoredCount in SettingsTabContent call
content = re.sub(
    r'onUpdateAvailable = \{\s*updateInfo ->\s*updateInfoToPrompt = updateInfo\s*\}',
    r'onUpdateAvailable = { updateInfo ->\n                updateInfoToPrompt = updateInfo\n            },\n            ignoredCount = ignoredCount',
    content
)

# Add the widget at the end of SettingsTabContent
widget_code = """
        // =========================================================================
        // REJECTED RIDES SUMMARY WIDGET
        // =========================================================================
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("rejected_rides_widget"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Rejected Rides",
                        tint = Rose400,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Rides Filtered Today",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = ignoredCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Rose400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
"""
content = re.sub(
    r'Text\("Add"\)\n\s*\}\n\s*\}\n\s*\}\n\s*\}\n\s*\}\n\s*\}\n\}',
    r'Text("Add")\n                        }\n                    }\n                }\n            }\n' + widget_code + r'\n        }\n    }\n}',
    content
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

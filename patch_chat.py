import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Update signature of SettingsTabContent
content = content.replace(
    "onOpenHelpSupport: () -> Unit = {},",
    "onOpenHelpSupport: () -> Unit = {},\n    onOpenChatbot: () -> Unit = {},"
)

# 2. Update call of SettingsTabContent inside MainActivity.kt
content = content.replace(
    "onOpenHelpSupport = { showHelpScreen = true },",
    "onOpenHelpSupport = { showHelpScreen = true },\n            onOpenChatbot = { showChatbotScreen = true },"
)

# 3. Add showChatbotScreen to AutoAcceptDashboardScreen state
content = content.replace(
    "var showHelpScreen by remember { mutableStateOf(false) }",
    "var showHelpScreen by remember { mutableStateOf(false) }\n    var showChatbotScreen by remember { mutableStateOf(false) }"
)

# 4. Add the rendering block to the top of Scaffold
chatbot_block = """
        if (showChatbotScreen) {
            androidx.activity.compose.BackHandler { showChatbotScreen = false }
            com.example.ui.SupportChatbotScreen(
                onNavigateBack = { showChatbotScreen = false }
            )
            return
        }

"""
content = content.replace(
    "if (showHelpScreen) {",
    chatbot_block + "        if (showHelpScreen) {"
)

# 5. Add the AI Help & Support card to SettingsTabContent
chatbot_card = """
        // =========================================================================
        // AI HELP & SUPPORT CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenChatbot() }
                .padding(vertical = 8.dp)
                .testTag("ai_support_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = com.example.ui.Cyan400.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "AI Help & Support",
                        tint = com.example.ui.Cyan400,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Help & Support",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Chat with our smart assistant 24/7",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Slate500
                )
            }
        }
"""

help_support_search = """                    Text(
                        text = "FAQs, Troubleshooting & Automatic Guide",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Slate500
                )
            }
        }"""

if help_support_search in content:
    content = content.replace(help_support_search, help_support_search + "\n" + chatbot_card)
else:
    print("Could not find the help_support_search block.")

# Add missing import for Chat icon
if "import androidx.compose.material.icons.filled.Chat" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.ChevronRight", "import androidx.compose.material.icons.filled.ChevronRight\nimport androidx.compose.material.icons.filled.Chat")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add showHelpScreen state variable
state_vars_orig = """    var showRestrictedGuide by remember { mutableStateOf(false) }
    var showNotificationGuide by remember { mutableStateOf(false) }"""

state_vars_new = """    var showHelpScreen by remember { mutableStateOf(false) }
    var showRestrictedGuide by remember { mutableStateOf(false) }
    var showNotificationGuide by remember { mutableStateOf(false) }"""

content = content.replace(state_vars_orig, state_vars_new)

# Add screen rendering block before Scaffold
# Scaffold is around line 1050 usually, let's find it.
# We can just put it right after the permissions dialogs or at the top of the function.
# Look for "if (showHeuristicSheet)" block in AutoAcceptDashboardScreen. Wait, showHeuristicSheet was in AutoAcceptDashboardScreen.

render_block = """
        if (showHelpScreen) {
            androidx.activity.compose.BackHandler { showHelpScreen = false }
            com.example.ui.HelpSupportScreen(
                onNavigateBack = { showHelpScreen = false }
            )
            return
        }

    Scaffold(
"""

content = content.replace("    Scaffold(", render_block)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

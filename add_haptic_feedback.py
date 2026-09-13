import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports = """
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
"""
content = content.replace("import androidx.compose.ui.platform.LocalContext", imports.strip() + "\nimport androidx.compose.ui.platform.LocalContext")

# Add HapticFeedback hook
haptic_code = """
    val hapticFeedback = LocalHapticFeedback.current
    var initialSyncDone by remember { mutableStateOf(false) }
    var previousTripCount by remember { mutableStateOf(dailyTripCount) }

    LaunchedEffect(dailyTripCount) {
        if (!initialSyncDone) {
            initialSyncDone = true
            previousTripCount = dailyTripCount
            return@LaunchedEffect
        }
        if (dailyTripCount > previousTripCount) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousTripCount = dailyTripCount
    }
"""

content = content.replace("    val tripHistory by AutoAcceptService.tripHistory.collectAsStateWithLifecycle()", 
    "    val tripHistory by AutoAcceptService.tripHistory.collectAsStateWithLifecycle()\n" + haptic_code)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

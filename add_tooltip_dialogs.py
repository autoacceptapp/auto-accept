import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

dialogs_code = """
    var showDistanceInfo by remember { mutableStateOf(false) }
    var showPriceInfo by remember { mutableStateOf(false) }
    var showRapidoInfo by remember { mutableStateOf(false) }

    if (showDistanceInfo) {
        AlertDialog(
            onDismissRequest = { showDistanceInfo = false },
            containerColor = Slate900,
            title = { Text("Distance Filter", color = Slate50, fontWeight = FontWeight.Bold) },
            text = { Text("Automatically ignores rides that have a pickup location further than your specified distance. This prevents you from wasting time driving far just to pick up a passenger.", color = Slate300, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showDistanceInfo = false }) { Text("Got it", color = Cyan400) } }
        )
    }
    
    if (showPriceInfo) {
        AlertDialog(
            onDismissRequest = { showPriceInfo = false },
            containerColor = Slate900,
            title = { Text("Price & Fare Filter", color = Slate50, fontWeight = FontWeight.Bold) },
            text = { Text("Only accepts orders where the fare amount falls between your minimum and maximum range. Orders below the minimum or above the maximum are automatically skipped.", color = Slate300, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showPriceInfo = false }) { Text("Got it", color = Cyan400) } }
        )
    }

    if (showRapidoInfo) {
        AlertDialog(
            onDismissRequest = { showRapidoInfo = false },
            containerColor = Slate900,
            title = { Text("Strict Rapido Filter", color = Slate50, fontWeight = FontWeight.Bold) },
            text = { Text("Ensures the bot only interacts with the official Rapido app. When enabled, it will verify the screen package name and ignore popups or buttons from other applications.", color = Slate300, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showRapidoInfo = false }) { Text("Got it", color = Cyan400) } }
        )
    }
"""

content = content.replace("    var isCheckingUpdates by remember { mutableStateOf(false) }", "    var isCheckingUpdates by remember { mutableStateOf(false) }\n" + dialogs_code)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

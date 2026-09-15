import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add onOpenHelpSupport to SettingsTabContent signature
content = content.replace(
    "onOpenHeuristicSheet: () -> Unit = {},",
    "onOpenHeuristicSheet: () -> Unit = {},\n    onOpenHelpSupport: () -> Unit = {},"
)

# Also update the call to SettingsTabContent
# Search for onOpenHeuristicSheet = { showHeuristicSheet = true },
content = content.replace(
    "onOpenHeuristicSheet = { showHeuristicSheet = true },",
    "onOpenHeuristicSheet = { showHeuristicSheet = true },\n            onOpenHelpSupport = { showHelpScreen = true },"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

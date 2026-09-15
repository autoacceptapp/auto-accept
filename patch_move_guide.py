import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove the old declaration
content = content.replace("    var showRestrictedGuide by remember { mutableStateOf(false) }\n", "")

# Add it just inside AutoAcceptDashboardScreen
# I'll insert it right after val context = LocalContext.current
content = content.replace(
    "val context = LocalContext.current",
    "val context = LocalContext.current\n    var showRestrictedGuide by remember { mutableStateOf(false) }"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


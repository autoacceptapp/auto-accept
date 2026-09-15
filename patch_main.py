import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add import if missing
if "import android.os.Build" not in content:
    content = content.replace("import android.os.Bundle", "import android.os.Bundle\nimport android.os.Build")

# Add state
if "var showRestrictedGuide by remember" not in content:
    content = content.replace("var showHeuristicSheet by remember { mutableStateOf(false) }", "var showHeuristicSheet by remember { mutableStateOf(false) }\n    var showRestrictedGuide by remember { mutableStateOf(false) }")

# Add dialog block
dialog_code = """
        if (showRestrictedGuide) {
            RestrictedSettingsGuideDialog(
                onDismiss = { showRestrictedGuide = false },
                onOpenAppInfo = {
                    showRestrictedGuide = false
                    openAppInfoSettings(context)
                },
                onOpenAccessibility = {
                    showRestrictedGuide = false
                    openAccessibilitySettings(context)
                }
            )
        }
"""
if "RestrictedSettingsGuideDialog(" not in content:
    content = content.replace("if (showHeuristicSheet) {", dialog_code + "\n        if (showHeuristicSheet) {")

# Replace onOpenAccessibility
old_call = "onOpenAccessibility = { openAccessibilitySettings(context) }"
new_call = """onOpenAccessibility = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showRestrictedGuide = true
                } else {
                    openAccessibilitySettings(context)
                }
            }"""
content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Add state variables to AutoAcceptDashboardScreen
state_vars = """    var showRestrictedGuide by remember { mutableStateOf(false) }
    var showNotificationGuide by remember { mutableStateOf(false) }
    var showAutoStartGuide by remember { mutableStateOf(false) }
    var showOverlayGuide by remember { mutableStateOf(false) }"""

content = content.replace("    var showRestrictedGuide by remember { mutableStateOf(false) }", state_vars)

# 2. Render the dialogs at the bottom of AutoAcceptDashboardScreen
# We can find "if (showRestrictedGuide) {"
dialogs = """        if (showRestrictedGuide) {
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
        
        if (showNotificationGuide) {
            NotificationAccessGuideDialog(
                onDismiss = { showNotificationGuide = false },
                onProceed = {
                    showNotificationGuide = false
                    openNotificationListenerSettings(context)
                }
            )
        }
        if (showAutoStartGuide) {
            AutoStartGuideDialog(
                onDismiss = { showAutoStartGuide = false },
                onProceed = {
                    showAutoStartGuide = false
                    openAutoStartSettings(context)
                }
            )
        }
        if (showOverlayGuide) {
            DisplayOverlayGuideDialog(
                onDismiss = { showOverlayGuide = false },
                onProceed = {
                    showOverlayGuide = false
                    openOverlaySettings(context)
                }
            )
        }"""

# Find the exact original RestrictedGuide block and replace with all dialogs
orig_restricted_guide_block = """        if (showRestrictedGuide) {
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
        }"""

if orig_restricted_guide_block in content:
    content = content.replace(orig_restricted_guide_block, dialogs)
else:
    print("Warning: orig_restricted_guide_block not found!")

# 3. Intercept MissingPermissionsDialog
content = content.replace("onOpenOverlay = { openOverlaySettings(context) }", "onOpenOverlay = { showOverlayGuide = true }")

# 4. Intercept DebugLogsScreen
content = content.replace("onOpenNotificationListener = { openNotificationListenerSettings(context) }", "onOpenNotificationListener = { showNotificationGuide = true }")

# 5. Add onOpenAutoStart to SettingsTabContent signature and update its call
if "onOpenAutoStart: () -> Unit = {}," not in content:
    content = content.replace("onOpenOverlay: () -> Unit,", "onOpenOverlay: () -> Unit,\n    onOpenAutoStart: () -> Unit = {},")

# In SettingsTabContent call:
if "onOpenAutoStart = { showAutoStartGuide = true }," not in content:
    content = content.replace("onOpenOverlay = { showOverlayGuide = true },", "onOpenOverlay = { showOverlayGuide = true },\n            onOpenAutoStart = { showAutoStartGuide = true },")

# Inside SettingsTabContent replace openAutoStartSettings(context) with onOpenAutoStart()
content = content.replace("onClick = { openAutoStartSettings(context) },", "onClick = onOpenAutoStart,")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

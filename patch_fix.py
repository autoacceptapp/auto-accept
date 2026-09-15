import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Fix MissingPermissionsDialog
missing_perms_block_pattern = r"""        MissingPermissionsDialog\(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isOverlayAllowed = isOverlayAllowed,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            onOpenAccessibility = \{
                if \(Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.TIRAMISU\) \{
                    showRestrictedGuide = true
                \} else \{
                    openAccessibilitySettings\(context\)
                \}
            \},
            onOpenOverlay = \{ showOverlayGuide = true \},
            onOpenAutoStart = \{ showAutoStartGuide = true \},
            onOpenBatteryOptimization = \{ openBatteryOptimizationSettings\(context\) \},
            onDismiss = \{ userDismissedPermissionsDialog = true \}
        \)"""

missing_perms_block_fixed = """        MissingPermissionsDialog(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isOverlayAllowed = isOverlayAllowed,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            onOpenAccessibility = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showRestrictedGuide = true
                } else {
                    openAccessibilitySettings(context)
                }
            },
            onOpenOverlay = { showOverlayGuide = true },
            onOpenBatteryOptimization = { openBatteryOptimizationSettings(context) },
            onDismiss = { userDismissedPermissionsDialog = true }
        )"""

content = re.sub(missing_perms_block_pattern, missing_perms_block_fixed, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

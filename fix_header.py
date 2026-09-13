import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("fun SettingsSectionHeader(title: String) {", "fun SettingsSectionHeader(title: String, showDivider: Boolean = true) {")
content = content.replace("androidx.compose.material3.HorizontalDivider(color = Slate800, thickness = 1.dp)", "if (showDivider) androidx.compose.material3.HorizontalDivider(color = Slate800, thickness = 1.dp)")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

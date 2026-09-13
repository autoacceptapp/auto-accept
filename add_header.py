import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

header_composable = """@Composable
fun SettingsSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        androidx.compose.material3.HorizontalDivider(color = Slate800, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Cyan400,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

"""

if "fun SettingsSectionHeader(" not in content:
    content = content.replace("fun SettingsTabContent(", header_composable + "fun SettingsTabContent(")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

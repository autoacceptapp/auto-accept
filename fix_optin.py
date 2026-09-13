import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\nfun SettingsTabContent(", "@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n@Composable\nfun SettingsTabContent(")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.SupportAgent" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Settings", 
                              "import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.SupportAgent\nimport androidx.compose.material.icons.filled.ChevronRight")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

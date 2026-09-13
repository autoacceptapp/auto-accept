import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material.icons.filled.Warning", "import androidx.compose.material.icons.filled.Warning\nimport androidx.compose.material.icons.filled.Info")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

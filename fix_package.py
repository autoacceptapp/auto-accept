with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.graphics.Brush\npackage com.example", "package com.example\nimport androidx.compose.ui.graphics.Brush")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

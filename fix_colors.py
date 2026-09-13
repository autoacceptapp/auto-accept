with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("private val Emerald400 = Color(0xFF34D399)", "private val Emerald400 = Color(0xFF34D399)\nprivate val Emerald600 = Color(0xFF059669)")
content = content.replace("import androidx.compose.foundation.layout.Column", "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.fillMaxHeight")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

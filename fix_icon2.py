import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("imageVector = Icons.Default.Search", "imageVector = androidx.compose.material.icons.filled.Search")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

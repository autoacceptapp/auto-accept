import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.ui.Cyan400", "Cyan400")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

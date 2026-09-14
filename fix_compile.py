import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Fix the Regex
content = content.replace('Regex("₹(\\\\d+)")', 'Regex("₹([0-9]+)")')
content = content.replace('Regex("₹(\\\d+)")', 'Regex("₹([0-9]+)")')
content = content.replace('Regex("₹(\\d+)")', 'Regex("₹([0-9]+)")')

# Fix Emerald900
content = content.replace('color = Emerald900.copy(alpha = 0.3f),', 'color = androidx.compose.ui.graphics.Color(0xFF064E3B).copy(alpha = 0.3f),')

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

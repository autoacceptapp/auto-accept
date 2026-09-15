import os
import re

files = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/DashboardTabs.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()

    # Find the when block and replace monthly -> 30 with monthly -> 28
    content = content.replace('"monthly" -> 30', '"monthly" -> 28')
    
    with open(file, "w") as f:
        f.write(content)


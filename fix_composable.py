import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("fun SettingsTabContent(", "@Composable\nfun SettingsTabContent(")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

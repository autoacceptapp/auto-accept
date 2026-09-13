import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if "fun SettingsTabContent(" in line:
        start = i
        break

if start != -1:
    print(len(lines) - start)

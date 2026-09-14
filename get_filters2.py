import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

start_idx = content.find('SettingsSectionHeader("SMART FILTERS')
if start_idx != -1:
    end_idx = content.find('SettingsSectionHeader(', start_idx + 10)
    print(content[start_idx:end_idx])
else:
    print("Not found")

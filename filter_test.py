import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

start_idx = content.find('SettingsSectionHeader("SMART FILTERS')
if start_idx != -1:
    end_idx = content.find('SettingsSectionHeader("ADVANCED CONFIGURATION"', start_idx)
    print("Found! Lines:")
    print(content[:start_idx].count('\n'), "to", content[:end_idx].count('\n'))
else:
    print("Not found")

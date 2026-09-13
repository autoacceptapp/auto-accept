import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Before SYSTEM PERMISSIONS CARD
content = content.replace("        // 1. SYSTEM PERMISSIONS CARD", "        SettingsSectionHeader(\"PERMISSIONS & REQUIREMENTS\", showDivider = false)\n        // 1. SYSTEM PERMISSIONS CARD")

# 2. Before AUTO ACCEPT SERVICE & DELAY CONFIGURATION
content = content.replace("        // 2. AUTO ACCEPT SERVICE & DELAY CONFIGURATION", "        SettingsSectionHeader(\"CORE AUTOMATION\")\n        // 2. AUTO ACCEPT SERVICE & DELAY CONFIGURATION")

# 3. Before DISTANCE FILTER CARD
content = content.replace("            // 2. DISTANCE FILTER CARD (PREMIUM FEATURE)", "        SettingsSectionHeader(\"SMART FILTERS (PREMIUM)\")\n            // 2. DISTANCE FILTER CARD (PREMIUM FEATURE)")

# 4. Before VOICE ANNOUNCER
content = content.replace("        // 3. VOICE ANNOUNCER (TTS) CARD", "        SettingsSectionHeader(\"VOICE & ALERTS\")\n        // 3. VOICE ANNOUNCER (TTS) CARD")

# 5. Before ADVANCED INTERACTION SETTINGS CARD
content = content.replace("        // 6. ADVANCED INTERACTION SETTINGS CARD", "        SettingsSectionHeader(\"ADVANCED & UPDATES\")\n        // 6. ADVANCED INTERACTION SETTINGS CARD")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

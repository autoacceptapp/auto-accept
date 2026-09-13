import re

with open("app/src/main/java/com/example/DashboardTabs.kt", "r") as f:
    content = f.read()

# Change default tab to 1 (Accepted)
content = content.replace("var selectedFilterTab by rememberSaveable { mutableStateOf(0) } // 0: All, 1: Accepted, 2: Ignored", "var selectedFilterTab by rememberSaveable { mutableStateOf(1) } // 0: All, 1: Accepted, 2: Ignored")

# Limit acceptedLogs to 10
content = content.replace("1 -> acceptedLogs", "1 -> acceptedLogs.take(10)")

# Update label
content = content.replace("text = \"Accepted (${acceptedLogs.size})\",", "text = \"Recent Accepted\",")

with open("app/src/main/java/com/example/DashboardTabs.kt", "w") as f:
    f.write(content)

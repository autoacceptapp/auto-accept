with open("app/src/main/java/com/example/DashboardTabs.kt", "r") as f:
    content = f.read()

content = content.replace("                    enabled = !isApplyingReferral,\n", "")

with open("app/src/main/java/com/example/DashboardTabs.kt", "w") as f:
    f.write(content)

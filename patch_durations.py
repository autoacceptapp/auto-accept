import os
import re

files = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/DashboardTabs.kt",
    "app/src/main/java/com/example/SubscriptionManager.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()

    # PlanOption update
    content = content.replace('PlanOption("monthly", "Monthly", "30 Days", 179, SubscriptionManager.POINTS_MONTHLY_PASS, "SAVE 35%")', 
                              'PlanOption("monthly", "Monthly", "28 Days", 179, SubscriptionManager.POINTS_MONTHLY_PASS, "SAVE 35%")')

    # duration calculations
    content = content.replace('"monthly" -> 30 * 24 * 3600 * 1000L', '"monthly" -> 28L * 24 * 3600 * 1000L')
    content = content.replace('"monthly" -> Pair(30 * 24 * 3600 * 1000L, 179)', '"monthly" -> Pair(28L * 24 * 3600 * 1000L, 179)')
    
    with open(file, "w") as f:
        f.write(content)


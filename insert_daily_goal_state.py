import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("    val dailyTripCount by AutoAcceptService.dailyTripCount.collectAsStateWithLifecycle()",
    "    val dailyTripCount by AutoAcceptService.dailyTripCount.collectAsStateWithLifecycle()\n    var dailyGoal by remember { mutableStateOf(AutoAcceptService.getDailyGoal(context)) }")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

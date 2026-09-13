import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

sync_addition = """
        fun syncDailyTripCount(context: Context) {
            _dailyTripCount.value = getDailyTripCount(context)
            _tripHistory.value = getTripHistory(context)
        }
"""
content = re.sub(r"        fun syncDailyTripCount\(context: Context\) \{.*?\n        \}", sync_addition, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
print("Removed mock data")

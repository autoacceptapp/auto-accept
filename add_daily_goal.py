import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

content = content.replace('const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"', 'const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"\n        const val KEY_DAILY_GOAL = "key_daily_goal"')

getter_setter = """
        fun getDailyGoal(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_DAILY_GOAL, 10) // Default 10
        }

        fun setDailyGoal(context: Context, goal: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
        }
"""
content = content.replace('fun getDailyTripCount(context: Context): Int {', getter_setter.lstrip() + '\n        fun getDailyTripCount(context: Context): Int {')

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

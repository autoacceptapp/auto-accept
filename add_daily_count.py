import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add the SharedPreferences keys
keys_addition = """
        const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"
        const val KEY_LAST_TRIP_DATE = "key_last_trip_date"
"""
content = content.replace("const val KEY_LOCAL_RIDE_LOGS = \"key_local_ride_logs\"", "const val KEY_LOCAL_RIDE_LOGS = \"key_local_ride_logs\"" + keys_addition)


methods_addition = """
        fun getDailyTripCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_TRIP_DATE, "")
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            if (lastDate != currentDate) {
                // Reset counter for new day
                prefs.edit().putInt(KEY_DAILY_TRIP_COUNT, 0).putString(KEY_LAST_TRIP_DATE, currentDate).apply()
                return 0
            }
            return prefs.getInt(KEY_DAILY_TRIP_COUNT, 0)
        }

        fun incrementDailyTripCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = getDailyTripCount(context)
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            prefs.edit()
                .putInt(KEY_DAILY_TRIP_COUNT, currentCount + 1)
                .putString(KEY_LAST_TRIP_DATE, currentDate)
                .apply()
        }
"""
# Insert before getSharedPreferences getters
content = content.replace("        // SharedPreferences Getters & Setters", "        // SharedPreferences Getters & Setters" + methods_addition)


with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

print("Added counter functions to AutoAcceptService")

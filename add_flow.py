import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

flow_addition = """
        private val _dailyTripCount = MutableStateFlow(0)
        val dailyTripCount: StateFlow<Int> = _dailyTripCount.asStateFlow()
"""
content = content.replace("val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()", "val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()" + flow_addition)

# In incrementDailyTripCount, update the flow
update_logic = """
            prefs.edit()
                .putInt(KEY_DAILY_TRIP_COUNT, currentCount + 1)
                .putString(KEY_LAST_TRIP_DATE, currentDate)
                .apply()
            _dailyTripCount.value = currentCount + 1
"""
content = re.sub(r"prefs\.edit\(\)\s*\.putInt\(KEY_DAILY_TRIP_COUNT,\s*currentCount\s*\+\s*1\)\s*\.putString\(KEY_LAST_TRIP_DATE,\s*currentDate\)\s*\.apply\(\)", update_logic, content)

# In onServiceConnected or init, fetch the initial value for the flow
# Let's add a sync method
sync_method = """
        fun syncDailyTripCount(context: Context) {
            _dailyTripCount.value = getDailyTripCount(context)
        }
"""
content = content.replace("fun incrementDailyTripCount(context: Context) {", sync_method + "\n        fun incrementDailyTripCount(context: Context) {")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

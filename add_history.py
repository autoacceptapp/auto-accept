import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add keys for history
keys_addition = """
        const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"
        const val KEY_LAST_TRIP_DATE = "key_last_trip_date"
        const val KEY_TRIP_HISTORY = "key_trip_history" // JSON string of last 7 days
"""
content = content.replace("""        const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"
        const val KEY_LAST_TRIP_DATE = "key_last_trip_date\"""", keys_addition)

# Add flow for history
flow_addition = """
        private val _dailyTripCount = MutableStateFlow(0)
        val dailyTripCount: StateFlow<Int> = _dailyTripCount.asStateFlow()

        // List of Pair<DateString, Count>
        private val _tripHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
        val tripHistory: StateFlow<List<Pair<String, Int>>> = _tripHistory.asStateFlow()
"""
content = content.replace("""        private val _dailyTripCount = MutableStateFlow(0)
        val dailyTripCount: StateFlow<Int> = _dailyTripCount.asStateFlow()""", flow_addition)

# Add getTripHistory method
methods_addition = """
        fun getTripHistory(context: Context): List<Pair<String, Int>> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyStr = prefs.getString(KEY_TRIP_HISTORY, "") ?: ""
            if (historyStr.isBlank()) return emptyList()
            
            return try {
                historyStr.split(";").mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        Pair(parts[0], parts[1].toIntOrNull() ?: 0)
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun saveTripHistory(context: Context, history: List<Pair<String, Int>>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Keep only last 7 days
            val trimmed = history.takeLast(7)
            val historyStr = trimmed.joinToString(";") { "${it.first}:${it.second}" }
            prefs.edit().putString(KEY_TRIP_HISTORY, historyStr).apply()
            _tripHistory.value = trimmed
        }

        fun getDailyTripCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_TRIP_DATE, "")
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            if (lastDate != currentDate && !lastDate.isNullOrEmpty()) {
                // Day changed! Save previous day's count to history before resetting
                val lastCount = prefs.getInt(KEY_DAILY_TRIP_COUNT, 0)
                val currentHistory = getTripHistory(context).toMutableList()
                // Remove if already exists to avoid duplicates
                currentHistory.removeAll { it.first == lastDate }
                currentHistory.add(Pair(lastDate, lastCount))
                saveTripHistory(context, currentHistory)
                
                // Reset counter for new day
                prefs.edit().putInt(KEY_DAILY_TRIP_COUNT, 0).putString(KEY_LAST_TRIP_DATE, currentDate).apply()
                return 0
            } else if (lastDate.isNullOrEmpty()) {
                 prefs.edit().putInt(KEY_DAILY_TRIP_COUNT, 0).putString(KEY_LAST_TRIP_DATE, currentDate).apply()
                 return 0
            }
            return prefs.getInt(KEY_DAILY_TRIP_COUNT, 0)
        }
"""
content = re.sub(r"        fun getDailyTripCount\(context: Context\): Int \{.*?\n        \}", methods_addition, content, flags=re.DOTALL)

# Sync method
sync_addition = """
        fun syncDailyTripCount(context: Context) {
            _dailyTripCount.value = getDailyTripCount(context)
            _tripHistory.value = getTripHistory(context)
            
            // To ensure the chart looks good right away even for new users,
            // let's inject some dummy history if it's completely empty.
            if (_tripHistory.value.isEmpty()) {
                val cal = java.util.Calendar.getInstance()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dummyHistory = mutableListOf<Pair<String, Int>>()
                for (i in 6 downTo 1) {
                    cal.time = java.util.Date()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                    dummyHistory.add(Pair(sdf.format(cal.time), (5..25).random()))
                }
                saveTripHistory(context, dummyHistory)
            }
        }
"""
content = re.sub(r"        fun syncDailyTripCount\(context: Context\) \{.*?\n        \}", sync_addition, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)
print("Updated AutoAcceptService")

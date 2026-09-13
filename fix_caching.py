import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add sync methods
sync_code = """
        // =========================================================================
        // ROOM DATABASE SYNC (Offline Caching Strategy)
        // =========================================================================
        private fun syncSettingsToRoom(context: Context) {
            kotlin.concurrent.thread {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    db.settingsDao().insertFilterSettingsSync(
                        com.example.data.FilterSettings(
                            id = 1,
                            maxDistance = getMaxDistanceKm(context),
                            minPrice = getMinPrice(context),
                            maxPrice = getMaxPrice(context),
                            isDistanceFilterOn = isDistanceFilterEnabled(context),
                            isPriceFilterOn = isPriceFilterEnabled(context),
                            isBlacklistFilterOn = isBlacklistEnabled(context),
                            blacklistKeywords = getBlacklistKeywords(context)
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync filter settings to Room: ${e.message}")
                }
            }
        }

        private fun syncTripHistoryToRoom(context: Context, history: List<Pair<String, Int>>) {
            kotlin.concurrent.thread {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val records = history.map { com.example.data.TripHistoryRecord(it.first, it.second) }
                    db.settingsDao().clearTripHistorySync()
                    db.settingsDao().insertTripHistorySync(records)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync trip history to Room: ${e.message}")
                }
            }
        }
"""
content = content.replace("        // =========================================================================\n        // SharedPreferences Getters & Setters", sync_code + "\n        // =========================================================================\n        // SharedPreferences Getters & Setters")

# Sync trip history
content = content.replace("            _tripHistory.value = trimmed", "            _tripHistory.value = trimmed\n            syncTripHistoryToRoom(context, trimmed)")

# Sync filters
content = content.replace("prefs.edit().putFloat(KEY_MAX_DISTANCE_KM, distance).apply()", "prefs.edit().putFloat(KEY_MAX_DISTANCE_KM, distance).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putFloat(KEY_MIN_PRICE, price).apply()", "prefs.edit().putFloat(KEY_MIN_PRICE, price).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putFloat(KEY_MAX_PRICE, price).apply()", "prefs.edit().putFloat(KEY_MAX_PRICE, price).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putString(KEY_BLACKLIST_KEYWORDS, keywords).apply()", "prefs.edit().putString(KEY_BLACKLIST_KEYWORDS, keywords).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putBoolean(KEY_DISTANCE_FILTER_ENABLED, enabled).apply()", "prefs.edit().putBoolean(KEY_DISTANCE_FILTER_ENABLED, enabled).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putBoolean(KEY_PRICE_FILTER_ENABLED, enabled).apply()", "prefs.edit().putBoolean(KEY_PRICE_FILTER_ENABLED, enabled).apply()\n            syncSettingsToRoom(context)")
content = content.replace("prefs.edit().putBoolean(KEY_BLACKLIST_ENABLED, enabled).apply()", "prefs.edit().putBoolean(KEY_BLACKLIST_ENABLED, enabled).apply()\n            syncSettingsToRoom(context)")


with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

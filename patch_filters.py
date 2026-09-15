import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Update evaluateRideAgainstFilters to handle null pickup/drop
eval_old = """        if (isBlacklistFilterOn) {
            val blacklistCsv = getBlacklistKeywords(context)
            val keywords = blacklistCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (keyword in keywords) {
                if (ride.pickup.contains(keyword, true) || ride.drop.contains(keyword, true)) {
                    return FilterDecision(false, "Matched blacklisted keyword: $keyword", "BLACKLIST")
                }
            }
        }
        
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val activeRules = db.settingsDao().getActiveCustomRulesSync()
            if (activeRules.isNotEmpty()) {
                for (rule in activeRules) {
                    if (rule.isMinFareEnabled) {
                        if (ride.price == null || ride.price < rule.minFare) return FilterDecision(false, "Did not meet Custom Rule min fare", "CUSTOM_RULE")
                    }
                    if (rule.isMaxDistanceEnabled) {
                        if (ride.distanceKm == null || ride.distanceKm > rule.maxDistanceKm) return FilterDecision(false, "Did not meet Custom Rule max distance", "CUSTOM_RULE")
                    }
                    if (rule.isRatingFilterEnabled) {
                        if (ride.passengerRating == null || ride.passengerRating < rule.minPassengerRating) return FilterDecision(false, "Did not meet Custom Rule min rating", "CUSTOM_RULE")
                    }
                    if (rule.destinationKeyword.isNotBlank()) {
                        val matchesWord = ride.pickup.contains(rule.destinationKeyword, true) || ride.drop.contains(rule.destinationKeyword, true)
                        if (!matchesWord) return FilterDecision(false, "Did not match Custom Rule destination keyword", "CUSTOM_RULE")
                    }
                }
            }
        } catch (e: Exception) {"""
eval_new = """        if (isBlacklistFilterOn) {
            val blacklistCsv = getBlacklistKeywords(context)
            val keywords = blacklistCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (keyword in keywords) {
                val pickMatch = ride.pickup?.contains(keyword, true) == true
                val dropMatch = ride.drop?.contains(keyword, true) == true
                if (pickMatch || dropMatch) {
                    return FilterDecision(false, "Matched blacklisted keyword: $keyword", "BLACKLIST")
                }
            }
        }
        
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val activeRules = db.settingsDao().getActiveCustomRulesSync()
            if (activeRules.isNotEmpty()) {
                for (rule in activeRules) {
                    if (rule.isMinFareEnabled) {
                        if (ride.price == null || ride.price < rule.minFare) return FilterDecision(false, "Did not meet Custom Rule min fare", "CUSTOM_RULE")
                    }
                    if (rule.isMaxDistanceEnabled) {
                        if (ride.distanceKm == null || ride.distanceKm > rule.maxDistanceKm) return FilterDecision(false, "Did not meet Custom Rule max distance", "CUSTOM_RULE")
                    }
                    if (rule.isRatingFilterEnabled) {
                        if (ride.passengerRating == null || ride.passengerRating < rule.minPassengerRating) return FilterDecision(false, "Did not meet Custom Rule min rating", "CUSTOM_RULE")
                    }
                    if (rule.destinationKeyword.isNotBlank()) {
                        if (ride.pickup == null && ride.drop == null) {
                            return FilterDecision(false, "Required location missing for Custom Rule", "CUSTOM_RULE")
                        }
                        val pickMatch = ride.pickup?.contains(rule.destinationKeyword, true) == true
                        val dropMatch = ride.drop?.contains(rule.destinationKeyword, true) == true
                        if (!pickMatch && !dropMatch) {
                            return FilterDecision(false, "Did not match Custom Rule destination keyword", "CUSTOM_RULE")
                        }
                    }
                }
            }
        } catch (e: Exception) {"""
content = content.replace(eval_old, eval_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

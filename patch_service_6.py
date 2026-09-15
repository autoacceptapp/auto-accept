import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add evaluateRideAgainstFilters function
evaluator_code = """
    private fun evaluateRideAgainstFilters(context: Context, ride: RideData): FilterDecision {
        val isBlacklistFilterOn = isBlacklistEnabled(context)
        val isDistanceFilterOn = isDistanceFilterEnabled(context)
        val isPriceFilterOn = isPriceFilterEnabled(context)
        val isRatingFilterOn = isPassengerRatingFilterEnabled(context)
        
        // Mandatory distance check
        if (isDistanceFilterOn) {
            val maxDistance = getMaxDistance(context)
            if (ride.distanceKm == null) {
                return FilterDecision(false, "Distance unavailable", "DISTANCE")
            }
            if (maxDistance > 0f && ride.distanceKm > maxDistance) {
                return FilterDecision(false, "Pickup distance ${ride.distanceKm} km > Max $maxDistance km", "DISTANCE")
            }
        }
        
        // Mandatory price check
        if (isPriceFilterOn) {
            val minPrice = getMinPrice(context)
            val maxPrice = getMaxPrice(context)
            if (ride.price == null) {
                return FilterDecision(false, "Fare unavailable", "PRICE")
            }
            if (minPrice > 0f && ride.price < minPrice) {
                return FilterDecision(false, "Fare ₹${ride.price.toInt()} < Min ₹${minPrice.toInt()}", "PRICE")
            }
            if (maxPrice > 0f && ride.price > maxPrice) {
                return FilterDecision(false, "Fare ₹${ride.price.toInt()} > Max ₹${maxPrice.toInt()}", "PRICE")
            }
        }
        
        // Mandatory rating check
        if (isRatingFilterOn) {
            val minRating = getMinPassengerRating(context)
            if (ride.passengerRating == null) {
                return FilterDecision(false, "Passenger rating unavailable", "RATING")
            }
            if (ride.passengerRating < minRating) {
                return FilterDecision(false, "Passenger rating ${ride.passengerRating}★ < Min ${minRating}★", "RATING")
            }
        }
        
        // Blacklist check
        if (isBlacklistFilterOn) {
            val blacklist = getBlacklistedKeywords(context)
            for (keyword in blacklist) {
                if (keyword.isNotBlank() && (ride.pickup.contains(keyword, true) || ride.drop.contains(keyword, true))) {
                    return FilterDecision(false, "Matched blacklisted keyword: $keyword", "BLACKLIST")
                }
            }
        }
        
        // Custom Rule check (Room DB)
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val activeRules = db.settingsDao().getActiveCustomRulesSync()
            if (activeRules.isNotEmpty()) {
                // ALL active rules must pass (AND semantics)
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
        } catch (e: Exception) {
            Log.w(TAG, "Room custom rule check note: ${e.message}")
        }
        
        val reason = if (!isBlacklistFilterOn && !isDistanceFilterOn && !isPriceFilterOn && !isRatingFilterOn) {
            "Auto-Accepted (All Filters OFF - ${ride.delayMs}ms)"
        } else {
            "Auto-Accepted (Filters Passed - ${ride.delayMs}ms)"
        }
        
        return FilterDecision(true, reason, null)
    }
"""

if "fun evaluateRideAgainstFilters" not in content:
    content = content.replace("private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {", evaluator_code + "\n    private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

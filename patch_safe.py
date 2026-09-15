import os

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Inject classes at the top
classes = """data class RideData(
    val price: Float?,
    val distanceKm: Float?,
    val passengerRating: Float?,
    val pickup: String,
    val drop: String,
    val sourcePackage: String,
    val signature: String,
    val isPremium: Boolean,
    val delayMs: Long
)

data class FilterDecision(
    val passed: Boolean,
    val reason: String,
    val failedFilter: String? = null
)
"""
if "data class RideData(" not in content:
    content = content.replace("class AutoAcceptService : AccessibilityService(), TextToSpeech.OnInitListener {", classes + "\nclass AutoAcceptService : AccessibilityService(), TextToSpeech.OnInitListener {")

# 2. Inject evaluateRideAgainstFilters BEFORE processActiveWindow
evaluator = """    private fun evaluateRideAgainstFilters(context: Context, ride: RideData): FilterDecision {
        val isBlacklistFilterOn = isBlacklistEnabled(context)
        val isDistanceFilterOn = isDistanceFilterEnabled(context)
        val isPriceFilterOn = isPriceFilterEnabled(context)
        val isRatingFilterOn = isPassengerRatingFilterEnabled(context)
        
        if (isDistanceFilterOn) {
            val maxDistance = getMaxDistanceKm(context)
            if (ride.distanceKm == null) {
                return FilterDecision(false, "Distance unavailable", "DISTANCE")
            }
            if (maxDistance > 0f && ride.distanceKm > maxDistance) {
                return FilterDecision(false, "Pickup distance ${ride.distanceKm} km > Max $maxDistance km", "DISTANCE")
            }
        }
        
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
        
        if (isRatingFilterOn) {
            val minRating = getMinPassengerRating(context)
            if (ride.passengerRating == null) {
                return FilterDecision(false, "Passenger rating unavailable", "RATING")
            }
            if (ride.passengerRating < minRating) {
                return FilterDecision(false, "Passenger rating ${ride.passengerRating}★ < Min ${minRating}★", "RATING")
            }
        }
        
        if (isBlacklistFilterOn) {
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

    private fun processActiveWindow"""

if "evaluateRideAgainstFilters" not in content:
    content = content.replace("    private fun processActiveWindow", evaluator)

# 3. Replace processActiveWindow
start_idx = content.find("    private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {")
end_idx = content.find("    suspend fun findAndClickAcceptButton")
new_process = """    private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {
        val acceptButtons = findAllAcceptButtons(rootNode)
        
        if (acceptButtons.isEmpty()) {
            return
        }

        val isPremium = isPassActive(this)
        var bestRide: RideData? = null
        var bestButton: ValidatedButton? = null
        var bestBoxBounds = android.graphics.Rect()

        for (validButton in acceptButtons) {
            val cardContainer = findCardContainer(validButton.node)
            
            val cardTexts = extractAllScreenTexts(this, cardContainer)
            
            val parsedDistance = extractDistance(cardTexts)
            val parsedPrice = extractPrice(cardTexts)
            val parsedRating = extractPassengerRating(cardTexts)
            val parsedPickup = extractPickupLocation(cardTexts) ?: "Nearby Pickup"
            val parsedDrop = extractDropLocation(cardTexts) ?: "Destination Drop"

            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)

            val lastRejected = recentlyRejectedRides[rideSignature]
            if (lastRejected != null && System.currentTimeMillis() - lastRejected < REJECT_COOLDOWN_MS) continue

            val configuredDelay = getAcceptDelayMs(this)
            val delayMs = if (!isPremium) 1500L else configuredDelay

            val capturedRide = RideData(
                price = parsedPrice,
                distanceKm = parsedDistance,
                passengerRating = parsedRating,
                pickup = parsedPickup,
                drop = parsedDrop,
                sourcePackage = sourcePackage,
                signature = rideSignature,
                isPremium = isPremium,
                delayMs = delayMs
            )

            val filterDecision = evaluateRideAgainstFilters(this, capturedRide)

            if (!filterDecision.passed) {
                Log.i(TAG, filterDecision.reason)
                _recentLog.value = filterDecision.reason
                logServiceEvent(
                    type = ServiceEventType.ORDER_IGNORED,
                    title = "Order rejected (${filterDecision.failedFilter ?: "Filter"})",
                    description = filterDecision.reason,
                    details = "Drop: $parsedDrop",
                    badge = "REJECTED"
                )
                logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", filterDecision.reason, sourcePackage)
                recentlyRejectedRides[rideSignature] = System.currentTimeMillis()
                continue
            }

            // Select highest payout
            if (bestRide == null || (capturedRide.price ?: 0f) > (bestRide.price ?: 0f)) {
                bestRide = capturedRide
                bestButton = validButton
                val tBounds = android.graphics.Rect()
                cardContainer.getBoundsInScreen(tBounds)
                if (tBounds.isEmpty) tBounds.set(validButton.targetBounds)
                bestBoxBounds = tBounds
            }
        }

        if (bestRide != null && bestButton != null) {
            val capturedRide = bestRide
            val validButton = bestButton
            val delayMs = capturedRide.delayMs
            val isPremium = capturedRide.isPremium
            val sourcePackage = capturedRide.sourcePackage
            val parsedPrice = capturedRide.price
            val parsedDistance = capturedRide.distanceKm
            val fareInfo = parsedPrice?.let { "₹${it.toInt()}" } ?: "Fare ~"
            val distInfo = parsedDistance?.let { "${it} km" } ?: "Dist ~"
            val rideSignature = capturedRide.signature

            BoundingBoxManager.updateBox(bestBoxBounds)
            StatusOverlayManager.updateLastPrice(parsedPrice)
            updateForegroundNotification("Processing ride: $fareInfo ($distInfo)")

            if (isWakeLockEnabled(this)) {
                wakeUpScreenAndUnlock(this)
            }

            val engineMode = if (isPremium) "[PREMIUM ${delayMs}ms]" else "[FREE ${delayMs}ms]"
            _recentLog.value = "Accept scheduled in $sourcePackage [$fareInfo | $distInfo] $engineMode"
            logServiceEvent(
                type = ServiceEventType.ORDER_QUEUED,
                title = "Accept scheduled",
                description = "Tapping in ${delayMs}ms $engineMode",
                details = "$fareInfo • $distInfo",
                badge = "SCHEDULED"
            )

            pendingRideSignature = rideSignature
            pendingAcceptJob?.cancel() 
            pendingAcceptJob = serviceScopeInstance.launch {
                try {
                    if (delayMs > 0) delay(delayMs)

                    if (!isAutomationEnabled(this@AutoAcceptService)) {
                        return@launch
                    }

                    // BUG 7/9: REVALIDATE!
                    val newRoot = try { rootInActiveWindow } catch(e: Exception) { null }
                    if (newRoot == null) return@launch

                    val currentButtons = findAllAcceptButtons(newRoot)
                    val stillValid = currentButtons.any { 
                        val b = android.graphics.Rect()
                        it.node.getBoundsInScreen(b)
                        android.graphics.Rect.intersects(b, validButton.targetBounds)
                    }
                    if (!stillValid) {
                        Log.w(TAG, "Revalidation failed: Accept button disappeared or moved.")
                        _recentLog.value = "Accept cancelled: Screen changed"
                        return@launch
                    }

                    val recentAcceptedCheck = recentlyAcceptedRides[capturedRide.signature]
                    if (recentAcceptedCheck != null && System.currentTimeMillis() - recentAcceptedCheck < DUPLICATE_COOLDOWN_MS) {
                        Log.w(TAG, "Pending accept cancelled: duplicate cooldown active")
                        return@launch
                    }

                    if (isPremium && isVoiceOnlyMode(this@AutoAcceptService)) {
                        lastClickTimestamp = SystemClock.uptimeMillis()
                        isGenuineOrderIncoming = false
                        notificationResetJob?.cancel()
                        recentlyAcceptedRides[capturedRide.signature] = System.currentTimeMillis()
                        cleanStaleAcceptedRides()

                        val finalFare = capturedRide.price?.let { "₹${it.toInt()}" } ?: "Fare ~"
                        val finalDist = capturedRide.distanceKm?.let { "${it} km" } ?: "Dist ~"
                        val logMessage = "Passive Radar: Voice only announcement for $finalFare ($finalDist) to ${capturedRide.drop}"
                        Log.i(TAG, logMessage)
                        _recentLog.value = logMessage
                        updateForegroundNotification("Announced ride: $finalFare ($finalDist)")
                        logServiceEvent(
                            type = ServiceEventType.RIDE_DETECTED,
                            title = "Voice Announcement Only",
                            description = "Passive Radar announced ride for $finalFare ($finalDist)",
                            details = "Voice-only mode active: button click bypassed",
                            badge = "RADAR"
                        )
                        return@launch
                    }

                    // BUG 10: Change SUCCESS logging!
                    when (val outcome = executeAcceptClick(validButton)) {
                        is ClickResult.Success -> {
                            lastClickTimestamp = SystemClock.uptimeMillis()
                            isGenuineOrderIncoming = false
                            notificationResetJob?.cancel()
                            recentlyAcceptedRides[capturedRide.signature] = System.currentTimeMillis()
                            cleanStaleAcceptedRides()
                            incrementDailyTripCount(this@AutoAcceptService)

                            val finalFare = capturedRide.price?.let { "₹${it.toInt()}" } ?: "Fare ~"
                            val finalDist = capturedRide.distanceKm?.let { "${it} km" } ?: "Dist ~"

                            val logMessage = "Accept action dispatched in ${capturedRide.sourcePackage} [$finalFare | $finalDist] $engineMode"
                            Log.i(TAG, logMessage)
                            _recentLog.value = logMessage
                            updateForegroundNotification("Accept dispatched: $finalFare ($finalDist)")
                            logServiceEvent(
                                type = ServiceEventType.ORDER_ACCEPTED, // Keeping event type for dashboard
                                title = "Accept action dispatched",
                                description = "Tapped accept button for $finalFare ($finalDist)",
                                details = "Waiting for app to confirm",
                                badge = "DISPATCHED"
                            )
                            
                            triggerSuccessVibration()
                            playSuccessSound()
                            sendAutoAcceptNotification(
                                context = this@AutoAcceptService,
                                title = "Action Dispatched: $finalFare",
                                message = "Dist: $finalDist | Drop: ${capturedRide.drop}"
                            )

                            logRideLocally(
                                context = this@AutoAcceptService,
                                price = capturedRide.price ?: 0f,
                                pickupKm = capturedRide.distanceKm ?: 0f,
                                pickupLocation = capturedRide.pickup,
                                dropLocation = capturedRide.drop,
                                status = "DISPATCHED",
                                reason = "Action dispatched to UI",
                                sourcePackage = capturedRide.sourcePackage
                            )

                            if (capturedRide.isPremium && isTtsEnabled(this@AutoAcceptService)) {
                                val userName = getUserName(this@AutoAcceptService).ifBlank { DEFAULT_USER_NAME }
                                val announcement = generateOrderAnnouncement(
                                    context = this@AutoAcceptService,
                                    name = userName,
                                    price = capturedRide.price?.toInt(),
                                    distanceKm = capturedRide.distanceKm,
                                    dropLocation = capturedRide.drop
                                )
                                speak(announcement)
                            }
                        }
                        is ClickResult.Cancelled -> {
                            Log.w(TAG, "Click cancelled: ${outcome.reason}")
                        }
                        is ClickResult.Failed -> {
                            Log.e(TAG, "Click failed: ${outcome.reason}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pending accept execution: ${e.message}", e)
                } finally {
                    if (pendingRideSignature == capturedRide.signature) {
                        releaseCpuWakeLock("Pending accept completed or cancelled")
                    }
                }
            }
        }
    }

"""
content = content[:start_idx] + new_process + content[end_idx:]

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

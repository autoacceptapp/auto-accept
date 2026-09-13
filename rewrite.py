import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Replace processActiveWindow
process_active_window_regex = re.compile(r"private fun processActiveWindow\(rootNode: AccessibilityNodeInfo, sourcePackage: String\) \{.*?(?=\n    /\*\*\n     \* Traverses the accessibility node hierarchy iteratively to extract all nodes on screen\.)", re.DOTALL)

new_process_active_window = """private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {
        val acceptButtons = findAllAcceptButtons(rootNode)
        
        if (acceptButtons.isEmpty()) {
            return
        }

        acquireCpuWakeLock(timeoutMs = 15000L, reason = "Incoming ride detected on screen")

        for (validButton in acceptButtons) {
            val cardContainer = findCardContainer(validButton.node, maxLevels = 5)
            val cardTexts = extractAllScreenTexts(this, cardContainer)

            val parsedDistance = extractDistance(cardTexts)
            val parsedPrice = extractPrice(cardTexts)
            val parsedPickup = extractPickupLocation(cardTexts) ?: "Nearby Pickup"
            val parsedDrop = extractDropLocation(cardTexts) ?: "Destination Drop"

            val fareInfo = parsedPrice?.let { "₹${it.toInt()}" } ?: "Fare ~"
            val distInfo = parsedDistance?.let { "${it} km" } ?: "Dist ~"

            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)

            val lastAccepted = recentlyAcceptedRides[rideSignature]
            if (lastAccepted != null && System.currentTimeMillis() - lastAccepted < DUPLICATE_COOLDOWN_MS) {
                Log.d(TAG, "Duplicate ride already accepted recently: $rideSignature")
                continue
            }

            if (pendingAcceptJob?.isActive == true && pendingRideSignature == rideSignature) {
                continue
            }

            logServiceEvent(
                type = ServiceEventType.RIDE_DETECTED,
                title = "Ride detected",
                description = "$fareInfo • $distInfo • $parsedPickup → $parsedDrop",
                details = "Card texts: ${cardTexts.take(3).joinToString(", ")}",
                badge = "DETECTED"
            )

            val isPremium = isPassActive(this)
            val configuredDelay = getAcceptDelayMs(this)
            val delayMs: Long
            val acceptReason: String

            if (!isPremium) {
                delayMs = configuredDelay.coerceAtLeast(500L)
                acceptReason = "Auto-Accepted (Free Mode - ${delayMs}ms Delay)"
                Log.d(TAG, "Free Mode active: Skipping filters, queued with ${delayMs}ms delay")
            } else {
                delayMs = configuredDelay

                val isBlacklistFilterOn = isBlacklistEnabled(this)
                val isDistanceFilterOn = isDistanceFilterEnabled(this)
                val isPriceFilterOn = isPriceFilterEnabled(this)
                val areAllFiltersOff = !isBlacklistFilterOn && !isDistanceFilterOn && !isPriceFilterOn

                if (areAllFiltersOff) {
                    Log.d(TAG, "Accept All Override active: All individual filters OFF -> accepting without parameter checks.")
                    acceptReason = "Auto-Accepted (Premium Accept All - ${delayMs}ms)"
                } else {
                    var filterFailed = false
                    
                    if (isBlacklistFilterOn) {
                        val blacklistCsv = getBlacklistKeywords(this)
                        val matchedKeyword = findBlacklistedKeyword(cardTexts, blacklistCsv)
                        if (matchedKeyword != null) {
                            val logMsg = "Order REJECTED: Blacklisted keyword matched ('$matchedKeyword')"
                            Log.i(TAG, logMsg)
                            _recentLog.value = logMsg
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Order rejected (Blacklist)",
                                description = "Location matched '$matchedKeyword' ($fareInfo)",
                                details = "Drop: $parsedDrop",
                                badge = "REJECTED"
                            )
                            logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Blacklisted location: $matchedKeyword", sourcePackage)
                            filterFailed = true
                        }
                    }

                    if (!filterFailed && isDistanceFilterOn) {
                        val maxDistance = getMaxDistanceKm(this)
                        if (maxDistance > 0f && parsedDistance != null && parsedDistance > maxDistance) {
                            val logMsg = "Order REJECTED: Distance ${parsedDistance} km > Max ${maxDistance} km"
                            Log.i(TAG, logMsg)
                            _recentLog.value = logMsg
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Order rejected (Distance)",
                                description = "Pickup ${parsedDistance} km > Max ${maxDistance} km limit",
                                details = "Pickup: $parsedPickup",
                                badge = "REJECTED"
                            )
                            logRideLocally(this, parsedPrice ?: 0f, parsedDistance, parsedPickup, parsedDrop, "IGNORED", "Distance too high (${parsedDistance} km > ${maxDistance} km)", sourcePackage)
                            filterFailed = true
                        }
                    }

                    if (!filterFailed && isPriceFilterOn) {
                        val minPrice = getMinPrice(this)
                        val maxPrice = getMaxPrice(this)
                        if (parsedPrice != null) {
                            if (minPrice > 0f && parsedPrice < minPrice) {
                                val logMsg = "Order REJECTED: Fare ₹${parsedPrice.toInt()} < Min ₹${minPrice.toInt()}"
                                Log.i(TAG, logMsg)
                                _recentLog.value = logMsg
                                logServiceEvent(
                                    type = ServiceEventType.ORDER_IGNORED,
                                    title = "Order rejected (Fare Low)",
                                    description = "Fare ₹${parsedPrice.toInt()} < Min ₹${minPrice.toInt()}",
                                    details = "Pickup: $parsedPickup",
                                    badge = "REJECTED"
                                )
                                logRideLocally(this, parsedPrice, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Price below minimum (₹${parsedPrice.toInt()} < ₹${minPrice.toInt()})", sourcePackage)
                                filterFailed = true
                            } else if (maxPrice > 0f && parsedPrice > maxPrice) {
                                val logMsg = "Order REJECTED: Fare ₹${parsedPrice.toInt()} > Max ₹${maxPrice.toInt()}"
                                Log.i(TAG, logMsg)
                                _recentLog.value = logMsg
                                logServiceEvent(
                                    type = ServiceEventType.ORDER_IGNORED,
                                    title = "Order rejected (Fare High)",
                                    description = "Fare ₹${parsedPrice.toInt()} > Max ₹${maxPrice.toInt()}",
                                    details = "Pickup: $parsedPickup",
                                    badge = "REJECTED"
                                )
                                logRideLocally(this, parsedPrice, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Price above maximum (₹${parsedPrice.toInt()} > ₹${maxPrice.toInt()})", sourcePackage)
                                filterFailed = true
                            }
                        }
                    }

                    if (filterFailed) {
                        continue
                    }

                    acceptReason = "Auto-Accepted (Filters Passed - ${delayMs}ms)"
                }
            }

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

            val capturedRide = CapturedRide(
                sourcePackage = sourcePackage,
                price = parsedPrice,
                distanceKm = parsedDistance,
                pickup = parsedPickup,
                drop = parsedDrop,
                signature = rideSignature,
                acceptReason = acceptReason,
                isPremium = isPremium,
                delayMs = delayMs
            )

            pendingRideSignature = rideSignature
            pendingAcceptJob?.cancel() 

            pendingAcceptJob = serviceScopeInstance.launch {
                try {
                    if (delayMs > 0) delay(delayMs)

                    if (!isAutomationEnabled(this@AutoAcceptService)) {
                        return@launch
                    }

                    val recentAcceptedCheck = recentlyAcceptedRides[capturedRide.signature]
                    if (recentAcceptedCheck != null && System.currentTimeMillis() - recentAcceptedCheck < DUPLICATE_COOLDOWN_MS) {
                        Log.w(TAG, "Pending accept cancelled: duplicate cooldown active")
                        return@launch
                    }

                    when (val outcome = executeAcceptClick(validButton)) {
                        is ClickResult.Success -> {
                            lastClickTimestamp = SystemClock.uptimeMillis()
                            isGenuineOrderIncoming = false
                            notificationResetJob?.cancel()
                            recentlyAcceptedRides[capturedRide.signature] = System.currentTimeMillis()
                            cleanStaleAcceptedRides()

                            val finalFare = capturedRide.price?.let { "₹${it.toInt()}" } ?: "Fare ~"
                            val finalDist = capturedRide.distanceKm?.let { "${it} km" } ?: "Dist ~"
                            val logMessage = "Accepted order in ${capturedRide.sourcePackage} [$finalFare | $finalDist] $engineMode"
                            Log.i(TAG, logMessage)
                            _recentLog.value = logMessage

                            logServiceEvent(
                                type = ServiceEventType.ORDER_ACCEPTED,
                                title = "Order accepted",
                                description = "Successfully tapped accept button for $finalFare ($finalDist)",
                                details = "Auto-click executed via ${outcome.method} (${capturedRide.acceptReason})",
                                badge = if (capturedRide.isPremium) "FAST" else "ACCEPTED"
                            )
                            
                            triggerSuccessVibration()

                            logRideLocally(
                                context = this@AutoAcceptService,
                                price = capturedRide.price ?: 0f,
                                pickupKm = capturedRide.distanceKm ?: 0f,
                                pickupLocation = capturedRide.pickup,
                                dropLocation = capturedRide.drop,
                                status = "ACCEPTED",
                                reason = capturedRide.acceptReason,
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
                            _recentLog.value = "Click cancelled: ${outcome.reason}"
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Accept cancelled",
                                description = outcome.reason,
                                badge = "CANCELLED"
                            )
                        }
                        is ClickResult.Failed -> {
                            Log.e(TAG, "Click failed: ${outcome.reason}")
                            _recentLog.value = "Click failed: ${outcome.reason}"
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Click failed",
                                description = outcome.reason,
                                badge = "FAILED"
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Pending accept coroutine cancelled: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pending accept execution: ${e.message}", e)
                } finally {
                    if (pendingRideSignature == capturedRide.signature) {
                        releaseCpuWakeLock("Pending accept completed or cancelled")
                    }
                }
            }

            return
        }

        if (pendingAcceptJob?.isActive != true) {
            releaseCpuWakeLock("No valid ride cards found or all failed filters")
        }
    }
"""

content = process_active_window_regex.sub(new_process_active_window, content)

# Rename findAcceptButton -> findAllAcceptButtons and update logic
find_accept_button_regex = re.compile(r"private fun findAcceptButton\(rootNode: AccessibilityNodeInfo\): ValidatedButton\? \{.*?(?=\n    /\*\*\n     \* Scans the bottom 25%)", re.DOTALL)

new_find_all_accept_buttons = """private fun findCardContainer(node: AccessibilityNodeInfo, maxLevels: Int = 5): AccessibilityNodeInfo {
        var current: AccessibilityNodeInfo = node
        var depth = 0
        while (depth < maxLevels) {
            val parent = try { current.parent } catch (e: Exception) { null } ?: break
            current = parent
            depth++
        }
        return current
    }

    private fun findAllAcceptButtons(rootNode: AccessibilityNodeInfo): List<ValidatedButton> {
        val buttons = mutableListOf<ValidatedButton>()
        val exactKeywords = (getEnabledKeywords(this).toList() + listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "स्वीकार", "order le", "start ride")).distinct()
        for (keyword in exactKeywords) {
            val directNodes = try {
                rootNode.findAccessibilityNodeInfosByText(keyword)
            } catch (e: Exception) { emptyList() }

            for (node in directNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) continue

                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetBounds = Rect()
                    targetClickableNode.getBoundsInScreen(targetBounds)
                    if (targetBounds.isEmpty) node.getBoundsInScreen(targetBounds)

                    val isSwipe = keyword.contains("swipe", ignoreCase = true)
                    buttons.add(ValidatedButton(
                        node = targetClickableNode,
                        reason = "MacroDroid Fast-Path Match ('$keyword')",
                        actionType = if (isSwipe) ActionType.SWIPE else ActionType.CLICK,
                        targetBounds = targetBounds
                    ))
                }
            }
        }

        val allNodes = traverseAllNodes(rootNode)

        for (node in allNodes) {
            val nodePkg = node.packageName?.toString()
            if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                continue
            }

            val textCandidates = listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim()
            ).filter { it.isNotBlank() }

            for (text in textCandidates) {
                if (isValidAcceptText(this, text)) {
                    if (isNodeValidAcceptButton(node)) {
                        val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                        val targetPkg = targetClickableNode.packageName?.toString()
                        if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) {
                            continue
                        }

                        val targetBounds = Rect()
                        try {
                            targetClickableNode.getBoundsInScreen(targetBounds)
                            if (targetBounds.isEmpty || targetBounds.width() < 1 || targetBounds.height() < 1) {
                                node.getBoundsInScreen(targetBounds)
                            }
                        } catch (e: Exception) {
                            node.getBoundsInScreen(targetBounds)
                        }

                        val isSwipe = text.contains("swipe", ignoreCase = true)
                        val actionType = if (isSwipe) ActionType.SWIPE else ActionType.CLICK

                        val reason = if (isSwipe) {
                            "Fuzzy Keyword Swipe ('$text')"
                        } else {
                            "Fuzzy Keyword Accept ('$text')"
                        }

                        buttons.add(ValidatedButton(
                            node = targetClickableNode,
                            reason = reason,
                            actionType = actionType,
                            targetBounds = targetBounds
                        ))
                    }
                }
            }
        }

        for (viewId in ACCEPT_BUTTON_VIEW_IDS) {
            val matchingNodes = try {
                rootNode.findAccessibilityNodeInfosByViewId(viewId)
            } catch (e: Exception) {
                emptyList()
            }
            for (node in matchingNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                    continue
                }
                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetPkg = targetClickableNode.packageName?.toString()
                    if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) {
                        continue
                    }
                    val targetBounds = Rect()
                    targetClickableNode.getBoundsInScreen(targetBounds)
                    if (targetBounds.isEmpty) node.getBoundsInScreen(targetBounds)
                    buttons.add(ValidatedButton(
                        node = targetClickableNode,
                        reason = "View ID ($viewId)",
                        actionType = ActionType.CLICK,
                        targetBounds = targetBounds
                    ))
                }
            }
        }

        val displayHeight = resources.displayMetrics.heightPixels
        val rootBounds = Rect()
        try {
            rootNode.getBoundsInScreen(rootBounds)
        } catch (e: Exception) {}
        val screenHeight = if (!rootBounds.isEmpty && rootBounds.height() > 200) {
            rootBounds.height()
        } else {
            displayHeight
        }

        val bottomFallback = findBottomScreenHeuristicButton(allNodes, screenHeight)
        if (bottomFallback != null) {
            buttons.add(bottomFallback)
        }

        return buttons.distinctBy { it.targetBounds.toShortString() }
    }
"""

content = find_accept_button_regex.sub(new_find_all_accept_buttons, content)

# Update findAndClickAcceptButton
content = content.replace("val button = findAcceptButton(rootNode)", "val button = findAllAcceptButtons(rootNode).firstOrNull()")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDistanceExtraction() {
    val sampleTexts = listOf("Pickup at Indiranagar", "2.5 km away", "Trip estimate")
    val distance = AutoAcceptService.extractDistance(sampleTexts)
    assertEquals(2.5f, distance ?: 0f, 0.01f)

    val sampleTexts2 = listOf("Drop: Whitefield", "Trip: 8 km")
    val distance2 = AutoAcceptService.extractDistance(sampleTexts2)
    assertEquals(8.0f, distance2 ?: 0f, 0.01f)
  }

  @Test
  fun testPriceExtraction() {
    val sampleTexts = listOf("Est. Fare", "₹ 145", "Total Earnings")
    val price = AutoAcceptService.extractPrice(sampleTexts)
    assertEquals(145f, price ?: 0f, 0.01f)

    val sampleTextsRs = listOf("Rs. 85", "Pickup in 1.2 km")
    val priceRs = AutoAcceptService.extractPrice(sampleTextsRs)
    assertEquals(85f, priceRs ?: 0f, 0.01f)

    val sampleTextsInr = listOf("INR 220", "Drop 5km")
    val priceInr = AutoAcceptService.extractPrice(sampleTextsInr)
    assertEquals(220f, priceInr ?: 0f, 0.01f)
  }

  @Test
  fun testBlacklistLocationMatching() {
    val sampleTexts = listOf("Pickup: Indiranagar", "Drop: Bengaluru Airport T1", "Fare: ₹350")
    val matched = AutoAcceptService.findBlacklistedKeyword(sampleTexts, "Airport, Toll, Slum")
    assertEquals("Airport", matched)

    val cleanTexts = listOf("Pickup: Indiranagar", "Drop: Koramangala 5th Block", "Fare: ₹120")
    val noMatch = AutoAcceptService.findBlacklistedKeyword(cleanTexts, "Airport, Toll, Slum")
    assertNull(noMatch)

    // Case insensitive test
    val lowerCaseTexts = listOf("pickup near airport road", "drop: marathahalli")
    val matchedLower = AutoAcceptService.findBlacklistedKeyword(lowerCaseTexts, "AIRPORT, TOLL")
    assertEquals("AIRPORT", matchedLower)
  }

  @Test
  fun testDropLocationExtraction() {
    val sampleTexts = listOf("Pickup: Silk Board", "Drop: Whitefield ITPL", "₹ 210", "4.2 km")
    val drop = AutoAcceptService.extractDropLocation(sampleTexts)
    assertEquals("Whitefield ITPL", drop)

    val sampleTextsSeparated = listOf("Pickup", "Indiranagar", "Drop", "Koramangala 4th Block", "Accept")
    val drop2 = AutoAcceptService.extractDropLocation(sampleTextsSeparated)
    assertEquals("Koramangala 4th Block", drop2)
  }

  @Test
  fun testServiceEventsLogging() {
    AutoAcceptService.clearServiceEvents()
    assertTrue(AutoAcceptService.serviceEvents.value.isEmpty())

    AutoAcceptService.logServiceEvent(
      type = ServiceEventType.RIDE_DETECTED,
      title = "Ride detected",
      description = "₹150 • 3.2 km • Indiranagar → Koramangala",
      badge = "DETECTED"
    )

    assertEquals(1, AutoAcceptService.serviceEvents.value.size)
    val event = AutoAcceptService.serviceEvents.value.first()
    assertEquals("Ride detected", event.title)
    assertEquals(ServiceEventType.RIDE_DETECTED, event.type)
    assertEquals("DETECTED", event.badge)

    AutoAcceptService.logServiceEvent(
      type = ServiceEventType.ORDER_ACCEPTED,
      title = "Order accepted",
      description = "Auto-click executed for ₹150",
      badge = "ACCEPTED"
    )

    assertEquals(2, AutoAcceptService.serviceEvents.value.size)
    assertEquals("Order accepted", AutoAcceptService.serviceEvents.value[0].title)
    assertEquals("Ride detected", AutoAcceptService.serviceEvents.value[1].title)
  }

  @Test
  fun testExponentialBackoff_SuccessFirstAttempt() = kotlinx.coroutines.runBlocking {
    var callCount = 0
    val result = AutoAcceptService.retryWithExponentialBackoff(
      maxRetries = 3,
      initialDelayMs = 10L,
      maxDelayMs = 100L
    ) { attempt ->
      callCount++
      "SUCCESS"
    }
    assertEquals("SUCCESS", result)
    assertEquals(1, callCount)
  }

  @Test
  fun testExponentialBackoff_SuccessAfterRetries() = kotlinx.coroutines.runBlocking {
    var callCount = 0
    val retryDelays = mutableListOf<Long>()
    val result = AutoAcceptService.retryWithExponentialBackoff(
      maxRetries = 4,
      initialDelayMs = 10L,
      maxDelayMs = 200L,
      factor = 2.0,
      jitterFactor = 0.0,
      onRetry = { attempt, nextDelayMs, error ->
        retryDelays.add(nextDelayMs)
      }
    ) { attempt ->
      callCount++
      if (attempt < 3) {
        throw java.io.IOException("Temporary Firestore connectivity loss")
      }
      "CONNECTED"
    }
    assertEquals("CONNECTED", result)
    assertEquals(3, callCount)
    assertEquals(2, retryDelays.size)
    assertEquals(10L, retryDelays[0])
    assertEquals(20L, retryDelays[1])
  }

  @Test
  fun testExponentialBackoff_ExhaustsRetriesAndThrows() = kotlinx.coroutines.runBlocking {
    var callCount = 0
    try {
      AutoAcceptService.retryWithExponentialBackoff(
        maxRetries = 3,
        initialDelayMs = 5L,
        maxDelayMs = 50L
      ) { attempt ->
        callCount++
        throw java.io.IOException("Firestore unavailable")
      }
      fail("Expected exception to be thrown")
    } catch (e: java.io.IOException) {
      assertEquals("Firestore unavailable", e.message)
      assertEquals(3, callCount)
    }
  }

  @Test
  fun testRideSignatureConsistencyAndDeduplication() {
    val sig1 = AutoAcceptService.generateRideSignature(
      sourcePackage = AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE,
      price = 150f,
      distanceKm = 3.5f,
      pickup = "Indiranagar 100ft Rd",
      drop = "Koramangala Sony Signal"
    )

    val sig2 = AutoAcceptService.generateRideSignature(
      sourcePackage = AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE,
      price = 150f,
      distanceKm = 3.5f,
      pickup = "  Indiranagar 100ft Rd  ",
      drop = "Koramangala Sony Signal"
    )

    assertEquals(sig1, sig2)

    val sigDifferentPrice = AutoAcceptService.generateRideSignature(
      sourcePackage = AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE,
      price = 180f,
      distanceKm = 3.5f,
      pickup = "Indiranagar 100ft Rd",
      drop = "Koramangala Sony Signal"
    )
    assertNotEquals(sig1, sigDifferentPrice)
  }

  @Test
  fun testAcceptButtonValidation_FalsePositivePrevention() {
    // Valid accept texts
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Accept", keyword = "Accept"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "ACCEPT", keyword = "Accept"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Accept Order", keyword = "Accept"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Accept Ride", keyword = "Accept"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Swipe to Accept", keyword = "Swipe to Accept"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Take Order", keyword = "Take Order"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Confirm Order", keyword = "Confirm Order"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Go"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Chalo"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Shuru"))
    assertTrue(AutoAcceptService.isValidAcceptText(text = "Yes"))

    // False positives that MUST be rejected
    assertFalse(AutoAcceptService.isValidAcceptText(text = "Accept terms and conditions", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "We do not accept cash payments", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "Please accept privacy policy", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "Accept UPI only", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "Decline or cancel", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "Terms & conditions apply for acceptance", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "   ", keyword = "Accept"))
    assertFalse(AutoAcceptService.isValidAcceptText(text = "A very very long paragraph containing the word accept inside it somewhere that is not a button", keyword = "Accept"))
  }

  @Test
  fun testExportDebugLogsToCsv() {
    val sampleLogs = listOf(
      DebugLogEntry(
        id = "test_log_1",
        serviceOrigin = ServiceOrigin.NOTIFICATION,
        severity = LogSeverity.WARNING,
        category = OrderDebugCategory.ORDER_MISSED,
        title = "Missed: Price below minimum",
        message = "Ride fare of ₹45 is lower than threshold ₹60",
        fare = 45f,
        distanceKm = 2.1f,
        pickup = "Indiranagar, 100ft Road",
        drop = "MG Road, Metro Station",
        missedReason = "Fare ₹45 below threshold",
        suggestedFix = "Adjust Min Price setting in Home tab"
      )
    )

    val csv = DebugLogManager.exportLogsToCsv(sampleLogs)
    assertTrue(csv.startsWith("\uFEFF")) // Verify UTF-8 BOM
    assertTrue(csv.contains("Log_ID,Timestamp_Millis,Formatted_Time,Service_Origin,Severity,Category,Title"))
    assertTrue(csv.contains("test_log_1"))
    assertTrue(csv.contains("\"Indiranagar, 100ft Road\"")) // Proper CSV quoting for commas
    assertTrue(csv.contains("45.00"))
    assertTrue(csv.contains("Adjust Min Price setting in Home tab"))
  }

  @Test
  fun testExportRideLogsToCsv() {
    val sampleRideLogs = listOf(
      RideLogItem(
        id = "order_123",
        price = 150f,
        pickupKm = 3.2f,
        pickupLocation = "Koramangala 4th Block",
        dropLocation = "Indiranagar Club, 12th Main",
        status = "ACCEPTED",
        reason = "Matched filters"
      )
    )

    val csv = AutoAcceptService.exportRideLogsToCsv(sampleRideLogs)
    assertTrue(csv.startsWith("\uFEFF")) // Verify UTF-8 BOM
    assertTrue(csv.contains("Order_ID,Timestamp_Millis,Date_Time,Status,Fare_INR,Distance_KM,Pickup_Location,Drop_Location"))
    assertTrue(csv.contains("order_123"))
    assertTrue(csv.contains("ACCEPTED"))
    assertTrue(csv.contains("150.00"))
    assertTrue(csv.contains("3.2"))
    assertTrue(csv.contains("\"Indiranagar Club, 12th Main\""))
  }

  @Test
  fun testServiceStatusLabels() {
    fun getStatusLabel(isActive: Boolean): String = if (isActive) "Active" else "Inactive"

    assertEquals("Active", getStatusLabel(true))
    assertEquals("Inactive", getStatusLabel(false))
  }

  @Test
  fun testCpuWakeLockSafelyHandlesInactiveState() {
    assertFalse(AutoAcceptService.isCpuWakeLockHeld())
    // Ensure safe no-op release does not throw
    AutoAcceptService.releaseCpuWakeLock("Test clean release")
    assertFalse(AutoAcceptService.isCpuWakeLockHeld())
  }

  @Test
  fun testSuccessStreakTracking() {
    AutoAcceptService.resetSuccessStreak()
    assertEquals(0, AutoAcceptService.successStreak.value)

    // Consecutive accepted rides build the streak
    AutoAcceptService.recordAcceptedStreak()
    assertEquals(1, AutoAcceptService.successStreak.value)
    assertEquals(1, AutoAcceptService.bestSuccessStreak.value)

    AutoAcceptService.recordAcceptedStreak()
    assertEquals(2, AutoAcceptService.successStreak.value)
    assertEquals(2, AutoAcceptService.bestSuccessStreak.value)

    AutoAcceptService.recordAcceptedStreak()
    assertEquals(3, AutoAcceptService.successStreak.value)
    assertEquals(3, AutoAcceptService.bestSuccessStreak.value)

    // A missed ride resets current streak to 0, while best streak is preserved
    AutoAcceptService.resetSuccessStreak()
    assertEquals(0, AutoAcceptService.successStreak.value)
    assertEquals(3, AutoAcceptService.bestSuccessStreak.value)

    // A new accepted ride starts streak at 1, best streak remains 3
    AutoAcceptService.recordAcceptedStreak()
    assertEquals(1, AutoAcceptService.successStreak.value)
    assertEquals(3, AutoAcceptService.bestSuccessStreak.value)

    // Test automatic streak tracking via logServiceEvent
    AutoAcceptService.logServiceEvent(
      type = ServiceEventType.ORDER_ACCEPTED,
      title = "Order accepted",
      description = "Fast accept"
    )
    assertEquals(2, AutoAcceptService.successStreak.value)

    AutoAcceptService.logServiceEvent(
      type = ServiceEventType.ORDER_IGNORED,
      title = "Ride missed / filtered",
      description = "Missed ride"
    )
    assertEquals(0, AutoAcceptService.successStreak.value)
    assertEquals(3, AutoAcceptService.bestSuccessStreak.value)
  }

  @Test
  fun testHeatmapHourlyDemandCalculations() {
    val now = System.currentTimeMillis()
    val sampleRideLogs = listOf(
      RideLogItem(id = "1", price = 120f, createdMillis = now),
      RideLogItem(id = "2", price = 180f, createdMillis = now),
      RideLogItem(id = "3", price = 250f, createdMillis = now - 24 * 60 * 60 * 1000L) // Yesterday
    )

    // Verify hourly matrix distribution
    val matrix = Array(7) { IntArray(24) }
    val cal = java.util.Calendar.getInstance()
    val dayMillis = 24 * 60 * 60 * 1000L

    for (log in sampleRideLogs) {
      val diffDays = ((now - log.createdMillis) / dayMillis).toInt()
      if (diffDays in 0..6) {
        val dayIdx = 6 - diffDays
        cal.timeInMillis = log.createdMillis
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        matrix[dayIdx][hour]++
      }
    }

    // Today (dayIndex 6) should have 2 rides
    cal.timeInMillis = now
    val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    assertEquals(2, matrix[6][currentHour])
    // Yesterday (dayIndex 5) should have 1 ride
    assertEquals(1, matrix[5][currentHour])
  }
}

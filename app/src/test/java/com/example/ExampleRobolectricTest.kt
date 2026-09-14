package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Auto Accept", appName)
  }

  @Test
  fun testRemoteConfigFaqParsing() {
    val sampleJson = """
      [
        {
          "id": "test_faq_1",
          "category": "Permissions",
          "question": "Why does service stop?",
          "answer": "Android battery optimization kills background tasks.",
          "stepGuide": ["Open Settings", "Disable Battery Saver", "Re-enable Service"],
          "directAction": "OPEN_BATTERY"
        },
        {
          "id": "test_faq_2",
          "category": "Detection",
          "question": "How to speed up detection?",
          "answer": "Lower the debounce delay to 250ms.",
          "stepGuide": ["Go to Settings", "Select 250ms delay"],
          "directAction": "OPEN_ACCESSIBILITY"
        }
      ]
    """.trimIndent()

    val parsed = RemoteConfigManager.parseFaqs(sampleJson)
    assertEquals(2, parsed.size)
    assertEquals("test_faq_1", parsed[0].id)
    assertEquals("Permissions", parsed[0].category)
    assertEquals("Why does service stop?", parsed[0].question)
    assertEquals(3, parsed[0].stepGuide.size)
    assertEquals("OPEN_BATTERY", parsed[0].directAction)

    assertEquals("test_faq_2", parsed[1].id)
    assertEquals("Detection", parsed[1].category)
    assertEquals(2, parsed[1].stepGuide.size)
  }

  @Test
  fun testRemoteConfigFaqFallback() {
    // If empty or invalid JSON is provided, fallback default FAQs must be returned
    val fallbackFromEmpty = RemoteConfigManager.parseFaqs("")
    assertTrue(fallbackFromEmpty.isNotEmpty())
    assertTrue(fallbackFromEmpty.any { it.id == "faq_battery_kill" })
    assertTrue(fallbackFromEmpty.any { it.category == "Battery & OS" })

    val fallbackFromMalformed = RemoteConfigManager.parseFaqs("{ invalid json }")
    assertTrue(fallbackFromMalformed.isNotEmpty())
    assertTrue(fallbackFromMalformed.any { it.id == "faq_restricted_settings" })
  }

  @Test
  fun testAdbCommandGeneration() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cmd = getAdbCommandForDirectToggle(context)
    assertTrue(cmd.contains("adb shell pm grant"))
    assertTrue(cmd.contains("android.permission.WRITE_SECURE_SETTINGS"))
    assertTrue(cmd.contains(context.packageName))
  }

  @Test
  fun testDirectAccessibilityDisableExecution() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Should run safely without crashing even when service instance is not running
    val result = disableAccessibilityServiceDirectly(context)
    // Result is boolean indicating whether disable completed
    // Simply asserting that it runs cleanly without throwing unhandled exceptions
    org.junit.Assert.assertNotNull(result)
  }

  @Test
  fun testExtractPassengerRating() {
    val sampleList1 = listOf("Rapido Captain Ride", "Pickup: Koramangala (1.8 km)", "Drop: Indiranagar", "Fare: ₹185", "Passenger: Rahul (★ 4.85) 120 rides")
    val rating1 = AutoAcceptService.extractPassengerRating(sampleList1)
    assertEquals(4.85f, rating1 ?: 0f, 0.01f)

    val sampleList2 = listOf("Pickup: HSR Layout", "Rating: 4.2", "Fare: 90")
    val rating2 = AutoAcceptService.extractPassengerRating(sampleList2)
    assertEquals(4.2f, rating2 ?: 0f, 0.01f)

    val sampleList3 = listOf("No rating info here", "Fare: 100")
    val rating3 = AutoAcceptService.extractPassengerRating(sampleList3)
    assertEquals(null, rating3)
  }

  @Test
  fun testRoomDatabaseFilterPreferences() = kotlinx.coroutines.runBlocking {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db = com.example.data.AppDatabase.getDatabase(context)
      val dao = db.settingsDao()

      // 1. Test FilterSettings persistence
      val settings = com.example.data.FilterSettings(
        minPrice = 75f,
        maxPrice = 500f,
        maxDistance = 4.5f,
        isPriceFilterOn = true,
        isDistanceFilterOn = true,
        minPassengerRating = 4.6f,
        isPassengerRatingFilterOn = true
      )
      dao.insertFilterSettings(settings)
      val retrieved = dao.getFilterSettingsSync()
      org.junit.Assert.assertNotNull(retrieved)
      assertEquals(75f, retrieved!!.minPrice, 0.01f)
      assertEquals(4.5f, retrieved.maxDistance, 0.01f)
      assertEquals(4.6f, retrieved.minPassengerRating, 0.01f)
      assertTrue(retrieved.isPassengerRatingFilterOn)

      // 2. Test CustomFilterRule persistence
      val rule = com.example.data.CustomFilterRule(
        name = "High Surge Downtown",
        minFare = 200f,
        maxDistanceKm = 6.0f,
        minPassengerRating = 4.7f,
        destinationKeyword = "Downtown",
        isActive = true
      )
      val ruleId = dao.insertCustomRule(rule)
      assertTrue(ruleId > 0)

      val activeRules = dao.getActiveCustomRulesSync()
      assertTrue(activeRules.any { it.name == "High Surge Downtown" })

      // 3. Test toggle and deletion
      dao.toggleCustomRule(ruleId, false)
      val activeAfterToggle = dao.getActiveCustomRulesSync()
      assertTrue(activeAfterToggle.none { it.id == ruleId })

      dao.deleteCustomRule(ruleId)
      val allRulesAfterDelete = dao.getActiveCustomRulesSync()
      assertTrue(allRulesAfterDelete.none { it.id == ruleId })
    }
  }
}

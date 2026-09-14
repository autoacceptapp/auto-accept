import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Update recentlyAcceptedRides & cleanStaleAcceptedRides to include recentlyRejectedRides
reject_cache_code = """        private val recentlyAcceptedRides = ConcurrentHashMap<String, Long>()
        private const val DUPLICATE_COOLDOWN_MS = 25000L

        private val recentlyRejectedRides = ConcurrentHashMap<String, Long>()
        private const val REJECT_COOLDOWN_MS = 15000L

        private fun cleanStaleAcceptedRides() {
            val now = System.currentTimeMillis()
            val acceptCutoff = now - DUPLICATE_COOLDOWN_MS
            val acceptIt = recentlyAcceptedRides.entries.iterator()
            while (acceptIt.hasNext()) {
                if (acceptIt.next().value < acceptCutoff) {
                    acceptIt.remove()
                }
            }
            
            val rejectCutoff = now - REJECT_COOLDOWN_MS
            val rejectIt = recentlyRejectedRides.entries.iterator()
            while (rejectIt.hasNext()) {
                if (rejectIt.next().value < rejectCutoff) {
                    rejectIt.remove()
                }
            }
        }"""

content = re.sub(
    r'        private val recentlyAcceptedRides = ConcurrentHashMap<String, Long>\(\)\n        private const val DUPLICATE_COOLDOWN_MS = 25000L\n\n        private fun cleanStaleAcceptedRides\(\) \{.*?\n        \}',
    reject_cache_code,
    content,
    flags=re.MULTILINE | re.DOTALL
)

# 2. Add lastScanTimestamp (it might already be there, let's just make sure it's correct)
if "private var lastScanTimestamp: Long = 0" not in content:
    content = content.replace("private var lastClickTimestamp: Long = 0", "private var lastClickTimestamp: Long = 0\n    private var lastScanTimestamp: Long = 0")

# 3. Add Throttling in onAccessibilityEvent (if not already there properly)
throttle_code = """        val now = SystemClock.uptimeMillis()
        if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) {
            return
        }

        if (now - lastScanTimestamp < 1000L) return
        lastScanTimestamp = now"""

# Look for existing throttle or click cooldown check and replace it cleanly
content = re.sub(
    r'        val now = SystemClock.uptimeMillis\(\)\n        if \(now - lastClickTimestamp < CLICK_COOLDOWN_MS\) \{\n            return\n        \}(\n\n        // Throttle Spam Scans\n        if \(now - lastScanTimestamp < 1000L\) return\n        lastScanTimestamp = now)?',
    throttle_code,
    content
)

# 4. Strict Card Validation
strict_validation = """            val cardTexts = extractAllScreenTexts(this, cardContainer)

            val cardTextCombined = cardTexts.joinToString(" ").lowercase()
            val hasFareIndicator = Regex("(₹|rs|inr)").containsMatchIn(cardTextCombined)
            val hasDistanceIndicator = Regex("\\\\bkm\\\\b").containsMatchIn(cardTextCombined)
            if (!hasFareIndicator || !hasDistanceIndicator) {
                continue
            }"""
# Only replace if the strict validation isn't already there in exact form
content = re.sub(
    r'            val cardTexts = extractAllScreenTexts\(this, cardContainer\)\n\n            // STRICT CARD VALIDATION\n            val cardTextCombined = cardTexts\.joinToString\(" "\)\.lowercase\(\)\n            val hasFareIndicator = Regex\("\(₹\|rs\|inr\)"\)\.containsMatchIn\(cardTextCombined\)\n            val hasDistanceIndicator = Regex\("\\\\\\\\bkm\\\\\\\\b"\)\.containsMatchIn\(cardTextCombined\)\n            if \(\!hasFareIndicator \|\| \!hasDistanceIndicator\) \{\n                continue\n            \}',
    strict_validation,
    content
)
# Fallback in case it wasn't there
if "val hasDistanceIndicator =" not in content:
    content = content.replace(
        "            val cardTexts = extractAllScreenTexts(this, cardContainer)",
        strict_validation
    )

# 5. Add Reject Cache check
reject_check_code = """            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)

            val lastRejected = recentlyRejectedRides[rideSignature]
            if (lastRejected != null && System.currentTimeMillis() - lastRejected < REJECT_COOLDOWN_MS) continue"""
content = content.replace("            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)", reject_check_code)

# 6. Add Reject Cache update
reject_update_code = """                    if (filterFailed) {
                        recentlyRejectedRides[rideSignature] = System.currentTimeMillis()
                        continue
                    }"""
content = re.sub(
    r'                    if \(filterFailed\) \{\n                        continue\n                    \}',
    reject_update_code,
    content
)

# 7. Trigger Guard
trigger_code = """        fun triggerFromNotification(context: Context? = null) {
            val now = System.currentTimeMillis()
            if (now - genuineOrderIncomingTimestamp < 3000L) return
            genuineOrderIncomingTimestamp = now"""
content = re.sub(
    r'        fun triggerFromNotification\(context: Context\? = null\) \{\n            genuineOrderIncomingTimestamp = System.currentTimeMillis\(\)',
    trigger_code,
    content
)

# 8. Fix Keywords
content = re.sub(
    r'val exactKeywords = .*?listOf\("Accept", "Swipe to Accept", "Take Order", "Confirm", ".*?", "order le", "start ride"\)\)?(\.distinct\(\))?',
    'val exactKeywords = listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "order le", "start ride")',
    content
)

# 9. Delete evaluateRideScreenContext
content = re.sub(r'    private fun evaluateRideScreenContext.*?^    }', '', content, flags=re.MULTILINE | re.DOTALL)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

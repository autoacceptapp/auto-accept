import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 2. Add throttle in onAccessibilityEvent
content = content.replace(
"""        val now = SystemClock.uptimeMillis()
        if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) {
            return
        }""",
"""        val now = SystemClock.uptimeMillis()
        if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) {
            return
        }

        // Throttle Spam Scans
        if (now - lastScanTimestamp < 1000L) return
        lastScanTimestamp = now"""
)

# 3. Fix Keywords
content = content.replace(
"""val exactKeywords = (getEnabledKeywords(this).toList() + listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "स्वीकार", "order le", "start ride")).distinct()""",
"""val exactKeywords = listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "order le", "start ride")"""
)

# 4. Strict Card Validation
strict_validation = """            val cardTexts = extractAllScreenTexts(this, cardContainer)

            // STRICT CARD VALIDATION
            val cardTextCombined = cardTexts.joinToString(" ").lowercase()
            val hasFareIndicator = Regex("(₹|rs|inr)").containsMatchIn(cardTextCombined)
            val hasDistanceIndicator = Regex("\\\\bkm\\\\b").containsMatchIn(cardTextCombined)
            if (!hasFareIndicator || !hasDistanceIndicator) {
                continue
            }"""
            
content = content.replace(
"""            val cardTexts = extractAllScreenTexts(this, cardContainer)""",
strict_validation
)

# 5. Delete evaluateRideScreenContext
# If it's not found, this will just do nothing, which is fine as we checked and it's not there.
if "fun evaluateRideScreenContext" in content:
    content = re.sub(r"    private fun evaluateRideScreenContext.*?^    }", "", content, flags=re.MULTILINE | re.DOTALL)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

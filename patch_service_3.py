import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. RIDE_CONTEXT_INDICATORS
old_indicators = '''        val RIDE_CONTEXT_INDICATORS = listOf(
            "Pickup",
            "Pick up",
            "Drop",
            "Drop off",
            "₹",
            "Rs",
            "INR",
            "km",
            "KM",
            "Distance",
            "Fare",
            "Earnings",
            "Est. Fare",
            "Captain Fare"
        )'''

new_indicators = '''        val RIDE_CONTEXT_INDICATORS = listOf(
            "Pickup", "Pick up", "Drop", "Drop off", "₹", "Rs", "INR", "km", "KM", "Distance", "Fare", "Earnings", "Est. Fare", "Captain Fare"
        )'''
content = content.replace(old_indicators, new_indicators)

# 2. PRICE_REGEX check - should already be applied from earlier but let's re-apply just in case
old_price_regex = '''        val PRICE_REGEX = Regex(
            \"\"\"(?:(?:₹|Rs\.?|INR|Earn|Fare)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:₹|Rs\.?|INR))\"\"\",
            RegexOption.IGNORE_CASE
        )'''
# It's already what they asked for, but let's confirm it's exactly what they asked for.
new_price_regex = '''        val PRICE_REGEX = Regex(
            \"\"\"(?:(?:₹|Rs\.?|INR|Earn|Fare)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:₹|Rs\.?|INR))\"\"\",
            RegexOption.IGNORE_CASE
        )'''

# 5. parsedPrice log
old_log = '''            if (parsedPrice == null || parsedPrice <= 0f) {
                Log.w(TAG, "Fare not detected on screen. Ignored to prevent fake clicks.")
                continue // Skip: Real orders ALWAYS have a concrete fare amount!
            }'''
new_log = '''            if (parsedPrice == null || parsedPrice <= 0f) {
                Log.w(TAG, "Fare not detected (Parsed: ${parsedPrice}). Ignored to prevent fake clicks.")
                continue // Skip: Real orders ALWAYS have a concrete fare amount!
            }'''
content = content.replace(old_log, new_log)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

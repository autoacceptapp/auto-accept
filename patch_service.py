import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. PRICE_REGEX
old_price_regex = r"""        val PRICE_REGEX = Regex\(
            \"\"\"(?:\(?:₹\|Rs\.\\?\|INR\)\\s\*\(\\d\+\(?:\\.\\d\+\)\?\)\|\(\\d\+\(?:\\.\\d\+\)\?\)\\s\*(?:₹\|Rs\.\\?\|INR)\)\"\"\",
            RegexOption\.IGNORE_CASE
        \)"""

new_price_regex = """        val PRICE_REGEX = Regex(
            \"\"\"(?:(?:₹|Rs\\.?|INR|Earn|Fare)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:₹|Rs\\.?|INR))\"\"\",
            RegexOption.IGNORE_CASE
        )"""
content = re.sub(old_price_regex, new_price_regex, content)
# Try direct replacement if regex fails
if "val PRICE_REGEX = Regex(\n            \"\"\"(?:(?:₹|Rs\\.?|INR)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:₹|Rs\\.?|INR))\"\"\"," in content:
    content = content.replace("val PRICE_REGEX = Regex(\n            \"\"\"(?:(?:₹|Rs\\.?|INR)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:₹|Rs\\.?|INR))\"\"\",", "val PRICE_REGEX = Regex(\n            \"\"\"(?:(?:₹|Rs\\.?|INR|Earn|Fare)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:₹|Rs\\.?|INR))\"\"\",")

# 2. hasFareIndicator
content = content.replace(
    'val hasFareIndicator = Regex("(₹|rs|inr)").containsMatchIn(cardTextCombined)',
    'val hasFareIndicator = Regex("(₹|rs|inr|earn|fare)").containsMatchIn(cardTextCombined)'
)

# 3. eventPackage check
content = content.replace(
    "if (eventPackage != RAPIDO_CAPTAIN_PACKAGE) {",
    "if (!ALLOWED_RAPIDO_PACKAGES.contains(eventPackage)) {"
)

# 4. Debug warning
old_parsed_price = """            if (parsedPrice == null || parsedPrice <= 0f) {
                continue // Skip: Real orders ALWAYS have a concrete fare amount!
            }"""

new_parsed_price = """            if (parsedPrice == null || parsedPrice <= 0f) {
                Log.w(TAG, "Fare not detected on screen. Ignored to prevent fake clicks.")
                continue // Skip: Real orders ALWAYS have a concrete fare amount!
            }"""

content = content.replace(old_parsed_price, new_parsed_price)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

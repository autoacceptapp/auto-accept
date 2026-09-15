import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. PRICE_REGEX
old_price_regex = '''        val PRICE_REGEX = Regex(
            """(?:(?:₹|Rs\.?|INR|Earn|Fare)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:₹|Rs\.?|INR))""",
            RegexOption.IGNORE_CASE
        )'''
new_price_regex = '''        val PRICE_REGEX = Regex("""(?:(?:₹|Rs\.?|INR|Earn|Fare)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:₹|Rs\.?|INR))""", RegexOption.IGNORE_CASE)'''
content = content.replace(old_price_regex, new_price_regex)

# 2. onAccessibilityEvent ENTIRELY
# I will use a regex to match the entire function.
# It starts with `    override fun onAccessibilityEvent(event: AccessibilityEvent?) {` and ends before `    /**\n     * Inspects active window`
onAccEvent_pattern = r"    override fun onAccessibilityEvent\(event: AccessibilityEvent\?\) \{.*?    \/\*\*"
new_onAccEvent = """    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isAutomationEnabled(this)) return

        if (isGenuineOrderIncoming && System.currentTimeMillis() - genuineOrderIncomingTimestamp > 5000L) {
            isGenuineOrderIncoming = false
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val eventPackage = event.packageName?.toString() ?: ""
        if (eventPackage.isEmpty() || eventPackage == packageName || eventPackage == "com.android.systemui" || eventPackage == "com.google.android.apps.maps") {
            return
        }

        val isRapidoEvent = ALLOWED_RAPIDO_PACKAGES.contains(eventPackage)
        if (!isRapidoEvent && !isGenuineOrderIncoming) {
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) return
        if (now - lastScanTimestamp < 200L) return // Sped up to 200ms
        lastScanTimestamp = now

        val rapidoRoots = mutableListOf<AccessibilityNodeInfo>()
        try {
            val activeWindows = windows
            if (activeWindows != null) {
                for (window in activeWindows) {
                    val root = window.root
                    val pkg = root?.packageName?.toString()
                    if (pkg != null && ALLOWED_RAPIDO_PACKAGES.contains(pkg)) {
                        rapidoRoots.add(root)
                    }
                }
            }
        } catch (e: Exception) {}

        var windowRoot = try { event.source } catch (e: Exception) { null }
        while (windowRoot?.parent != null) {
            windowRoot = windowRoot.parent
        }
        val sourcePkg = windowRoot?.packageName?.toString() ?: eventPackage
        if (windowRoot != null && ALLOWED_RAPIDO_PACKAGES.contains(sourcePkg) && !rapidoRoots.contains(windowRoot)) {
            rapidoRoots.add(windowRoot)
        }

        if (rapidoRoots.isEmpty()) return

        for (root in rapidoRoots) {
            try {
                processActiveWindow(root, root.packageName?.toString() ?: eventPackage)
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating active window: ${e.message}", e)
            }
        }
    }

    /**"""
content = re.sub(onAccEvent_pattern, new_onAccEvent.replace('\\', '\\\\'), content, flags=re.DOTALL)

# 3. Remove strict fare/distance string checks
#    val cardTextCombined = cardTexts.joinToString(" ").lowercase()
#    val hasFareIndicator = Regex("(₹|rs|inr|earn|fare)").containsMatchIn(cardTextCombined)
#    val hasDistanceIndicator = Regex("\\bkm\\b").containsMatchIn(cardTextCombined)
#    if (!hasFareIndicator || !hasDistanceIndicator) {
#        continue
#    }
strict_checks_pattern = r'            val cardTextCombined = cardTexts\.joinToString\(" "\)\.lowercase\(\)\n            val hasFareIndicator = Regex\("\([^\)]+\)"\)\.containsMatchIn\(cardTextCombined\)\n            val hasDistanceIndicator = Regex\("\\\\bkm\\\\b"\)\.containsMatchIn\(cardTextCombined\)\n            if \(!hasFareIndicator \|\| !hasDistanceIndicator\) \{\n                continue\n            \}\n\n'
content = re.sub(strict_checks_pattern, '', content)

# Also try deleting if formatted slightly differently
content = content.replace('''            val cardTextCombined = cardTexts.joinToString(" ").lowercase()
            val hasFareIndicator = Regex("(₹|rs|inr|earn|fare)").containsMatchIn(cardTextCombined)
            val hasDistanceIndicator = Regex("\\bkm\\b").containsMatchIn(cardTextCombined)
            if (!hasFareIndicator || !hasDistanceIndicator) {
                continue
            }

''', '')

content = content.replace('''            val cardTextCombined = cardTexts.joinToString(" ").lowercase()
            val hasFareIndicator = Regex("( |rs|inr|earn|fare)").containsMatchIn(cardTextCombined)
            val hasDistanceIndicator = Regex("\\bkm\\b").containsMatchIn(cardTextCombined)
            if (!hasFareIndicator || !hasDistanceIndicator) {
                continue
            }

''', '')

# 4. Replace extractPrice ENTIRELY
extract_price_pattern = r"        fun extractPrice\(texts: List<String>\): Float\? \{.*?return null\n        \}"
new_extract_price = """        fun extractPrice(texts: List<String>): Float? {
            for (text in texts) {
                val match = PRICE_REGEX.find(text)
                if (match != null) {
                    val str1 = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    val str2 = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
                    val price = (str1 ?: str2)?.toFloatOrNull()
                    if (price != null) return price
                }
            }
            // Handle Split UI Nodes (Currency and Amount in different nodes)
            for ((index, text) in texts.withIndex()) {
                val lower = text.lowercase().trim()
                if (lower == "₹" || lower == "rs" || lower == "inr" || lower == "rs." || lower == "fare" || lower == "earn") {
                    val nextText = texts.getOrNull(index + 1)?.replace(",", "")?.trim()
                    val price = nextText?.toFloatOrNull()
                    if (price != null) return price
                }
            }
            return null
        }"""
content = re.sub(extract_price_pattern, new_extract_price.replace('\\', '\\\\'), content, flags=re.DOTALL)

# 5. Replace extractDistance ENTIRELY
extract_distance_pattern = r"        fun extractDistance\(texts: List<String>\): Float\? \{.*?return null\n        \}"
new_extract_distance = """        fun extractDistance(texts: List<String>): Float? {
            for (text in texts) {
                if (text.contains("pickup", ignoreCase = true) || text.contains("away", ignoreCase = true) || text.contains("pick up", ignoreCase = true)) {
                    val match = DISTANCE_REGEX.find(text)
                    if (match != null) {
                        val valueStr = match.groupValues.getOrNull(1)
                        val value = valueStr?.toFloatOrNull()
                        if (value != null) return value
                    }
                }
            }
            for (text in texts) {
                val match = DISTANCE_REGEX.find(text)
                if (match != null) {
                    val valueStr = match.groupValues.getOrNull(1)
                    val value = valueStr?.toFloatOrNull()
                    if (value != null) return value
                }
            }
            // Handle Split UI Nodes (Number and "km" in different nodes)
            for ((index, text) in texts.withIndex()) {
                val dist = text.trim().toFloatOrNull()
                if (dist != null) {
                    val nextText = texts.getOrNull(index + 1)?.lowercase()?.trim()
                    if (nextText == "km" || nextText == "kms") {
                        return dist
                    }
                }
            }
            return null
        }"""
content = re.sub(extract_distance_pattern, new_extract_distance.replace('\\', '\\\\'), content, flags=re.DOTALL)


with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)


import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Update onAccessibilityEvent
old_onacc = """        if (eventPackage.isEmpty() ||
            eventPackage == packageName ||
            eventPackage == "android" ||
            eventPackage == "com.android.systemui"
        ) {
            return
        }

        // Strict early return if event does not originate from allowed Rapido packages
        if (isTargetRapidoOnly(this) && !getEnabledApps(this@AutoAcceptService).contains(eventPackage)) {
            return
        }"""
new_onacc = """        if (eventPackage.isEmpty() ||
            eventPackage == packageName ||
            eventPackage == "android" ||
            eventPackage == "com.android.systemui" ||
            eventPackage == "com.google.android.apps.maps"
        ) {
            return
        }

        // Strict early return if event does not originate from allowed Rapido packages
        if (eventPackage != RAPIDO_CAPTAIN_PACKAGE) {
            return
        }"""
content = content.replace(old_onacc, new_onacc)

# 2. Update processActiveWindow nodePackage check
old_node_check = """        val nodePackage = targetNode.packageName?.toString() ?: eventPackage
        if (isTargetRapidoOnly(this) && !getEnabledApps(this@AutoAcceptService).contains(nodePackage)) {
            return
        }"""
new_node_check = """        val nodePackage = targetNode.packageName?.toString() ?: eventPackage
        if (nodePackage == "com.google.android.apps.maps" || nodePackage == "com.android.systemui") {
            return
        }
        if (nodePackage != RAPIDO_CAPTAIN_PACKAGE) {
            return
        }"""
content = content.replace(old_node_check, new_node_check)

# 3. Update parsedPrice check
old_price_check = """            val parsedDistance = extractDistance(cardTexts)
            val parsedPrice = extractPrice(cardTexts)
            val parsedRating = extractPassengerRating(cardTexts)"""
new_price_check = """            val parsedDistance = extractDistance(cardTexts)
            val parsedPrice = extractPrice(cardTexts)

            if (parsedPrice == null || parsedPrice <= 0f) {
                continue // Skip: Real orders ALWAYS have a concrete fare amount!
            }
            
            val parsedRating = extractPassengerRating(cardTexts)"""
content = content.replace(old_price_check, new_price_check)

# 4. Update findAllAcceptButtons
old_findall = """            val textCandidates = listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim()
            ).filter { it.isNotBlank() }

            for (text in textCandidates) {"""
new_findall = """            val textCandidates = listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim()
            ).filter { it.isNotBlank() }

            var isMapControl = false
            for (text in textCandidates) {
                val t = text.lowercase()
                if (t.contains("open step list") || t.contains("recenter") || t.contains("mute") || t.contains("route") || t.contains("navigation")) {
                    isMapControl = true
                    break
                }
            }
            if (isMapControl) continue

            for (text in textCandidates) {"""
content = content.replace(old_findall, new_findall)

# 5. Update findBottomScreenHeuristicButton
old_bottom = """            // Must reside in the bottom 25% of the screen
            if (bounds.centerY() < bottomThreshold && bounds.bottom < bottomThreshold) {
                continue
            }"""
new_bottom = """            val nodeText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase()
            if (nodeText.contains("open step list") || nodeText.contains("recenter") || nodeText.contains("mute") || nodeText.contains("route") || nodeText.contains("navigation")) {
                continue
            }

            // Must reside in the bottom 25% of the screen
            if (bounds.centerY() < bottomThreshold && bounds.bottom < bottomThreshold) {
                continue
            }"""
content = content.replace(old_bottom, new_bottom)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

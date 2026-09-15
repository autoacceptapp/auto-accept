import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Replace findCardContainer
findCard_pattern = r'    private fun findCardContainer\(node: AccessibilityNodeInfo, maxLevels: Int = 5\): AccessibilityNodeInfo \{.*?return current\n    \}'
new_findCard = """    private fun findCardContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var current: AccessibilityNodeInfo = node
        var depth = 0
        while (depth < 10) {
            val parent = try { current.parent } catch (e: Exception) { null } ?: break
            val className = parent.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                return current // Return the item inside the list, not the list itself
            }
            current = parent
            depth++
            
            try {
                val b = android.graphics.Rect()
                current.getBoundsInScreen(b)
                if (b.height() > 300 && b.width() > 300) {
                    return current // Meaningful card container size
                }
            } catch (e: Exception) {}
        }
        return current
    }"""
content = re.sub(findCard_pattern, new_findCard.replace('\\', '\\\\'), content, flags=re.DOTALL)

# 2. Replace isValidAcceptText fuzzy words
isValid_pattern = r'        fun isValidAcceptText\(context: Context\? = null, text: String, keyword: String = ""\): Boolean \{.*?return fuzzyRootPattern\.containsMatchIn\(text\)\n        \}'
new_isValid = """        fun isValidAcceptText(context: Context? = null, text: String, keyword: String = ""): Boolean {
            if (text.isBlank() || text.length > 35) return false
            val lower = text.lowercase()
            val falsePositiveWords = listOf("do not", "don't", "terms", "policy", "cash", "upi", "card", "condition", "decline", "reject", "cancel", "privacy", "return")
            if (falsePositiveWords.any { lower.contains(it) }) return false

            if (keyword.isNotBlank()) {
                if (text.equals(keyword, ignoreCase = true)) return true
                if (text.startsWith(keyword, ignoreCase = true)) return true
            }

            val enabledKeywords = if (context != null) getEnabledKeywords(context) else emptyList()
            if (enabledKeywords.any { text.equals(it, ignoreCase = true) || text.startsWith(it, ignoreCase = true) }) {
                return true
            }

            // Removed fuzzy dangerous words like "go", "yes", "chalo", "shuru"
            val pattern = Regex(""" + '"""^(?:swipe\\s+to\\s+accept|accept(?:\\s+(?:order|ride))?|take\\s+order|confirm\\s+order)(?:\\s*[\\(>→»\\d\\w\\s]*)?$"""' + """, RegexOption.IGNORE_CASE)
            if (pattern.matches(text)) return true

            val fuzzyRootPattern = Regex(""" + '"""\\b(?:accept|swipe|take|confirm)\\b"""' + """, RegexOption.IGNORE_CASE)
            return fuzzyRootPattern.containsMatchIn(text)
        }"""
content = re.sub(isValid_pattern, new_isValid.replace('\\', '\\\\'), content, flags=re.DOTALL)

# 3. Replace findAllAcceptButtons (remove bottom heuristic)
findAll_pattern = r'    private fun findAllAcceptButtons\(rootNode: AccessibilityNodeInfo\): List<ValidatedButton> \{.*?return buttons\.distinctBy \{ it\.targetBounds\.toShortString\(\) \}\n    \}'
new_findAll = """    private fun findAllAcceptButtons(rootNode: AccessibilityNodeInfo): List<ValidatedButton> {
        val buttons = mutableListOf<ValidatedButton>()
        val exactKeywords = listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "order le", "start ride")
        for (keyword in exactKeywords) {
            val directNodes = try {
                rootNode.findAccessibilityNodeInfosByText(keyword)
            } catch (e: Exception) { emptyList() }

            for (node in directNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) continue

                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetBounds = android.graphics.Rect()
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
            if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) continue

            val textCandidates = listOfNotNull(
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

            for (text in textCandidates) {
                if (isValidAcceptText(this, text)) {
                    if (isNodeValidAcceptButton(node)) {
                        val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                        val targetPkg = targetClickableNode.packageName?.toString()
                        if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) continue

                        val targetBounds = android.graphics.Rect()
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
                        val reason = if (isSwipe) "Fuzzy Keyword Swipe ('$text')" else "Fuzzy Keyword Accept ('$text')"

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
            } catch (e: Exception) { emptyList() }
            for (node in matchingNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) continue
                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetPkg = targetClickableNode.packageName?.toString()
                    if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) continue
                    
                    val targetBounds = android.graphics.Rect()
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

        // REMOVED findBottomScreenHeuristicButton to fix Bug 5 (too dangerous)

        return buttons.distinctBy { it.targetBounds.toShortString() }
    }"""
content = re.sub(findAll_pattern, new_findAll.replace('\\', '\\\\'), content, flags=re.DOTALL)

# 4. Remove findBottomScreenHeuristicButton
bottom_heuristic_pattern = r'    /\*\*\n     \* Scans the bottom 25% of the screen bounds.*?\n    \}'
content = re.sub(bottom_heuristic_pattern, '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)


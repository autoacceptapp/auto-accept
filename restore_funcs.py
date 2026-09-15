import os

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

new_funcs = """

    private fun findCardContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var current: AccessibilityNodeInfo = node
        var depth = 0
        while (depth < 10) {
            val parent = try { current.parent } catch (e: Exception) { null } ?: break
            val className = parent.className?.toString() ?: ""
            if (className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView")) {
                return current
            }
            current = parent
            depth++
            
            try {
                val b = android.graphics.Rect()
                current.getBoundsInScreen(b)
                if (b.height() > 300 && b.width() > 300) {
                    return current
                }
            } catch (e: Exception) {}
        }
        return current
    }

    private fun findAllAcceptButtons(rootNode: AccessibilityNodeInfo): List<ValidatedButton> {
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

        return buttons.distinctBy { it.targetBounds.toShortString() }
    }

    suspend fun findAndClickAcceptButton"""

if "private fun findCardContainer" not in content:
    content = content.replace("    suspend fun findAndClickAcceptButton", new_funcs)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

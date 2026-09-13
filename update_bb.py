import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add show() in onServiceConnected, next to StatusOverlayManager
content = re.sub(
    r"(StatusOverlayManager\.show\(this\))",
    r"\1\n        BoundingBoxManager.show(this)",
    content
)

# Add hide() in onDestroy
content = re.sub(
    r"(StatusOverlayManager\.hide\(\)\n\s*cancelPendingAcceptInternal\(\"Service destroyed\"\))",
    r"BoundingBoxManager.hide()\n        \1",
    content
)

# Add hide() in onUnbind
content = re.sub(
    r"(StatusOverlayManager\.hide\(\)\n\s*instance = null)",
    r"BoundingBoxManager.hide()\n        \1",
    content
)

# Add updateBox logic in processActiveWindow
injection = """val cardContainer = findCardContainer(validButton.node, maxLevels = 5)
            if (cardContainer != null) {
                val boxBounds = android.graphics.Rect()
                cardContainer.getBoundsInScreen(boxBounds)
                BoundingBoxManager.updateBox(boxBounds)
            } else {
                BoundingBoxManager.updateBox(validButton.targetBounds)
            }"""

content = content.replace("val cardContainer = findCardContainer(validButton.node, maxLevels = 5)", injection)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

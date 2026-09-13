import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add show() in onServiceConnected
content = re.sub(
    r"(Log\.i\(TAG, \"AutoAcceptService connected\..*?\n)",
    r"\1        StatusOverlayManager.show(this)\n",
    content
)

# Add hide() in onDestroy
content = re.sub(
    r"(cancelPendingAcceptInternal\(\"Service destroyed\"\))",
    r"StatusOverlayManager.hide()\n        \1",
    content
)

# Add hide() in onUnbind
content = re.sub(
    r"(instance = null\n\s*ServiceStatusNotificationManager\.updateStatus\(this\))",
    r"StatusOverlayManager.hide()\n        \1",
    content
)

# Add updateLastPrice in processActiveWindow
# Right after: val parsedPrice = extractPrice(cardTexts)
content = re.sub(
    r"(val parsedPrice = extractPrice\(cardTexts\))",
    r"\1\n            StatusOverlayManager.updateLastPrice(parsedPrice)",
    content
)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

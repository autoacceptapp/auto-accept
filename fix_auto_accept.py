import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Add lastScanTimestamp
content = content.replace("private var lastClickTimestamp: Long = 0", "private var lastClickTimestamp: Long = 0\n    private var lastScanTimestamp: Long = 0")

# 2. Add throttle in onAccessibilityEvent
throttle_code = """        val now = SystemClock.uptimeMillis()
        if (now - lastScanTimestamp < 1000L) return
        lastScanTimestamp = now"""
# Put it right below CLICK_COOLDOWN_MS ?
# Wait, CLICK_COOLDOWN_MS is a const. The prompt said: "Inside onAccessibilityEvent, right below CLICK_COOLDOWN_MS, add a 1-second scan throttle".
# But wait, there is a CLICK_COOLDOWN_MS check in onAccessibilityEvent:
#         val now = SystemClock.uptimeMillis()
#         if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) {
#             return
#         }

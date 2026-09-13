import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Replace the block that blocks on isGenuineOrderIncoming in onAccessibilityEvent
# It looks like:
#         // PRIMARY TRIGGER CHECK: The accessibility scanner ONLY runs when a real notification arrives
#         if (!isGenuineOrderIncoming) {
#             return
#         }
# 
#         // SAFETY CHECK: If flag has been stuck for > 5s, auto-reset and drop event
#         if (System.currentTimeMillis() - genuineOrderIncomingTimestamp > 5000L) {
#             Log.w(TAG, "onAccessibilityEvent: isGenuineOrderIncoming flag was stuck > 5s. Auto-resetting.")
#             isGenuineOrderIncoming = false
#             return
#         }

# I should replace this with a soft reset for the flag, but DO NOT return early, so it can reach processActiveWindow.
replacement = """
        // SAFETY CHECK: If flag has been stuck for > 5s, auto-reset
        if (isGenuineOrderIncoming && System.currentTimeMillis() - genuineOrderIncomingTimestamp > 5000L) {
            Log.w(TAG, "onAccessibilityEvent: isGenuineOrderIncoming flag was stuck > 5s. Auto-resetting.")
            isGenuineOrderIncoming = false
        }
"""

content = re.sub(
    r"        // PRIMARY TRIGGER CHECK:.*?(?=\n        // 2\. OVERLAY EVENT LISTENER)",
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

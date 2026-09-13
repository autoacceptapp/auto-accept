import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add lastScanTimestamp to the class level if not present
if "private var lastScanTimestamp: Long = 0" not in content:
    content = content.replace("private var lastClickTimestamp: Long = 0", "private var lastClickTimestamp: Long = 0\n    private var lastScanTimestamp: Long = 0")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

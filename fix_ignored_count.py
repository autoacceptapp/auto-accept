import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# At line ~793 we have: val serviceEvents by AutoAcceptService.serviceEvents.collectAsStateWithLifecycle()
# Let's add the ignoredCount calculation there.
ignored_count_calc = """    val serviceEvents by AutoAcceptService.serviceEvents.collectAsStateWithLifecycle()
    val totalIgnoredCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.ORDER_IGNORED }
    }"""
content = content.replace("    val serviceEvents by AutoAcceptService.serviceEvents.collectAsStateWithLifecycle()", ignored_count_calc)

# Fix the passed parameter
content = content.replace("            ignoredCount = ignoredCount", "            ignoredCount = totalIgnoredCount")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

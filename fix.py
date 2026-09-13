import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

replacement = """        fun initCache(context: Context) {
        }

        // Advanced Settings: Toggle Keywords and Apps
        fun getEnabledKeywords(context: Context): Set<String> {
            return setOf("Accept", "Swipe to Accept", "Take Order", "Confirm")
        }

        fun getEnabledApps(context: Context): Set<String> {
            return ALLOWED_RAPIDO_PACKAGES
        }"""

content = re.sub(
    r"        var cachedKeywords: Set<String>\? = null\n        var cachedApps: Set<String>\? = null\n\n        fun initCache\(context: Context\) \{.*?(?=        // Multi-Language TTS)",
    replacement + "\n\n",
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

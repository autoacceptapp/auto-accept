import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

methods_addition = """
        fun getCustomSoundUri(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CUSTOM_SOUND_URI, null)
        }

        fun setCustomSoundUri(context: Context, uri: String?) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CUSTOM_SOUND_URI, uri).apply()
        }
"""
content = re.sub(r"        // Voice Announcer \(TTS\) Toggle & User Name", methods_addition.lstrip() + "\n        // Voice Announcer (TTS) Toggle & User Name", content)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

sound_addition = """    private fun playSuccessSound() {
        try {
            val uriStr = getCustomSoundUri(this)
            if (!uriStr.isNullOrEmpty()) {
                val uri = android.net.Uri.parse(uriStr)
                val ringtone = android.media.RingtoneManager.getRingtone(this, uri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom sound: ${e.message}")
        }
    }
"""
content = content.replace("    private fun triggerSuccessVibration() {", sound_addition + "\n    private fun triggerSuccessVibration() {")

# Find where triggerSuccessVibration is called and call playSuccessSound there too
content = content.replace("                            triggerSuccessVibration()", "                            triggerSuccessVibration()\n                            playSuccessSound()")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

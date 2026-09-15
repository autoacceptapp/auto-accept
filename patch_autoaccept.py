import os
import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Fix free mode delay
old_delay_block = """            if (!isPremium) {
                delayMs = configuredDelay.coerceAtLeast(500L)
                acceptReason = "Auto-Accepted (Free Mode - ${delayMs}ms Delay)"
                Log.d(TAG, "Free Mode active: Skipping filters, queued with ${delayMs}ms delay")
            }"""

new_delay_block = """            if (!isPremium) {
                delayMs = 1500L
                acceptReason = "Auto-Accepted (Free Mode - 1.5s Delay)"
                Log.d(TAG, "Free Mode active: Skipping all filters and custom delay.")
            }"""

if old_delay_block in content:
    content = content.replace(old_delay_block, new_delay_block)

# Fix Voice Only mode checking
old_voice_only = """                    if (isVoiceOnlyMode(this@AutoAcceptService)) {"""
new_voice_only = """                    if (isPremium && isVoiceOnlyMode(this@AutoAcceptService)) {"""
if old_voice_only in content:
    content = content.replace(old_voice_only, new_voice_only)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)


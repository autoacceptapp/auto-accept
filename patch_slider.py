import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Fix the delay slider to be enabled = isPassActive
old_slider = """                    Slider(
                        value = acceptDelayMs.toFloat(),
                        onValueChange = { value ->
                            val rounded = (Math.round(value / 50.0) * 50).toLong()
                            onAcceptDelayChange(rounded)
                        },
                        valueRange = 0f..3000f,"""

new_slider = """                    Slider(
                        value = acceptDelayMs.toFloat(),
                        onValueChange = { value ->
                            if (isPassActive) {
                                val rounded = (Math.round(value / 50.0) * 50).toLong()
                                onAcceptDelayChange(rounded)
                            }
                        },
                        enabled = isPassActive,
                        valueRange = 0f..3000f,"""

content = content.replace(old_slider, new_slider)

old_preset = """                                onClick = { onAcceptDelayChange(presetMs) },"""
new_preset = """                                onClick = { if (isPassActive) onAcceptDelayChange(presetMs) },
                                enabled = isPassActive,"""

content = content.replace(old_preset, new_preset)


with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

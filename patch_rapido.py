import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_rapido = """                    Switch(
                        checked = isRapidoOnly,
                        onCheckedChange = onRapidoOnlyChange,
                        colors = SwitchDefaults.colors("""
new_rapido = """                    Switch(
                        checked = isRapidoOnly && isPassActive,
                        onCheckedChange = { if(isPassActive) onRapidoOnlyChange(it) },
                        enabled = isPassActive,
                        colors = SwitchDefaults.colors("""

content = content.replace(old_rapido, new_rapido)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


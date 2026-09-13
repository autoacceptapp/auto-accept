import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

content = content.replace("doori $distStr kilometer.", "pickup doori $distStr kilometer.")
content = content.replace("antar $distStr kilometer.", "pickup antar $distStr kilometer.")
content = content.replace("distance $distStr kilometers.", "pickup distance $distStr kilometers.")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

match = re.search(r'(private fun processActiveWindow.*?)(?=\n    /\*\*\n     \* Traverses the accessibility node hierarchy)', content, re.DOTALL)
if match:
    with open("processActiveWindow.kt", "w") as f:
        f.write(match.group(1))

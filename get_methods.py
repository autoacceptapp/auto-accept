import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# I want to read findCardContainer
match = re.search(r'    private fun findCardContainer\(.*?return current\n    \}', content, re.DOTALL)
if match:
    print("--- findCardContainer ---")
    print(match.group(0))

match = re.search(r'    suspend fun findAndClickAcceptButton.*?\}', content, re.DOTALL)
if match:
    print("--- findAndClickAcceptButton ---")
    print(match.group(0))


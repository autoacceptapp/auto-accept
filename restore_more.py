import os

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# I will extract them from service_dump.txt and inject them right before findCardContainer
with open("service_dump.txt", "r") as f:
    dump = f.read()

start_traverse = dump.find("    private fun traverseAllNodes")
end_traverse = dump.find("    private fun findCardContainer")
if start_traverse != -1 and end_traverse != -1:
    missing_funcs = dump[start_traverse:end_traverse]
    
    # insert before findCardContainer in the current file
    insert_idx = content.find("    private fun findCardContainer")
    if insert_idx != -1:
        content = content[:insert_idx] + missing_funcs + content[insert_idx:]

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

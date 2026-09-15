import re

with open("app/src/main/java/com/example/ui/PreferencesScreen.kt", "r") as f:
    content = f.read()


start_str = """
@Composable
fun FilterMetricPill("""

end_str = """
// =========================================================================
// 1. MINIMUM FARE PREFERENCE CARD
// =========================================================================
"""

idx_start = content.find(start_str)
idx_end = content.find(end_str)

if idx_start != -1 and idx_end != -1:
    content = content[:idx_start] + "\n" + content[idx_end:]

with open("app/src/main/java/com/example/ui/PreferencesScreen.kt", "w") as f:
    f.write(content)

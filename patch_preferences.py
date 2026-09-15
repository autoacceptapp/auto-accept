import re

with open("app/src/main/java/com/example/ui/PreferencesScreen.kt", "r") as f:
    content = f.read()

# Remove the usage of PreferencesOverviewCard inside PreferencesScreen
old_usage = """            // 1. OVERVIEW & ACTIVE STATUS BANNER
            PreferencesOverviewCard(
                filterSettings = filterSettings,
                activeCustomRulesCount = customRules.count { it.isActive }
            )

"""
if old_usage in content:
    content = content.replace(old_usage, "")

# Now let's remove the definition of PreferencesOverviewCard
start_str = """// =========================================================================
// OVERVIEW STATUS CARD
// =========================================================================
@Composable
fun PreferencesOverviewCard("""

end_str = """
@Composable
fun FilterMetricPill("""

idx_start = content.find(start_str)
idx_end = content.find(end_str)

if idx_start != -1 and idx_end != -1:
    content = content[:idx_start] + end_str + content[idx_end+len(end_str):]

with open("app/src/main/java/com/example/ui/PreferencesScreen.kt", "w") as f:
    f.write(content)

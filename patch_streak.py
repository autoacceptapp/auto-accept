import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Remove SuccessStreakHeaderCard
streak_header_pattern = r'''\s*SuccessStreakHeaderCard\(\n\s*bestStreak = bestSuccessStreak,\n\s*onResetClick = \{\n\s*AutoAcceptService\.resetSuccessStreak\(context\)\n\s*\}\n\s*\)'''
content = re.sub(streak_header_pattern, '', content)

streak_topbar_pattern = r'''\s*SuccessStreakTopBarBadge\(\n\s*bestStreak = bestSuccessStreak,\n\s*onResetClick = \{\n\s*AutoAcceptService\.resetSuccessStreak\(context\)\n\s*\}\n\s*\)'''
content = re.sub(streak_topbar_pattern, '', content)

heatmap_pattern = r'''\s*com\.example\.ui\.PeakHoursHeatmapCard\(\)'''
content = re.sub(heatmap_pattern, '', content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Patched MainActivity.kt")


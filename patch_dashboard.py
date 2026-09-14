import re

with open('app/src/main/java/com/example/DashboardTabs.kt', 'r') as f:
    content = f.read()

# Remove PeakHoursHeatmapCard from DashboardTabs.kt if present
heatmap_pattern = r'''\s*com\.example\.ui\.PeakHoursHeatmapCard\(\)'''
content = re.sub(heatmap_pattern, '', content)

with open('app/src/main/java/com/example/DashboardTabs.kt', 'w') as f:
    f.write(content)
print("Patched DashboardTabs.kt Heatmap")


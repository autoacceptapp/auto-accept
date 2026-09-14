import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

troubleshoot_pattern = r'''\s*// --- ACCESSIBILITY TROUBLESHOOTING & FAQ ---\s*\n\s*Spacer\(modifier = Modifier\.height\(24\.dp\)\)\n\s*com\.example\.ui\.AccessibilityFaqSection\(\)'''
content = re.sub(troubleshoot_pattern, '', content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Patched Troubleshoot in MainActivity.kt")

try:
    with open('app/src/main/java/com/example/DashboardTabs.kt', 'r') as f:
        content2 = f.read()
    content2 = re.sub(troubleshoot_pattern, '', content2)
    with open('app/src/main/java/com/example/DashboardTabs.kt', 'w') as f:
        f.write(content2)
    print("Patched Troubleshoot in DashboardTabs.kt")
except Exception as e:
    pass


with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

start = -1
end = -1
depth = 0
for i, line in enumerate(lines):
    if "fun SettingsTabContent(" in line:
        start = i
        depth = 0
    if start != -1:
        for char in line:
            if char == '{': depth += 1
            elif char == '}': depth -= 1
        if depth == 0 and '{' in line: pass
        if depth == 0 and i > start + 5:
            end = i
            break

print(f"SettingsTabContent is from {start} to {end}")

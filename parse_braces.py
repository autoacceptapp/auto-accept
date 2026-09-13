with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

depth = 0
started = False
start_line = 0

for i, line in enumerate(lines):
    if "fun AutoAcceptDashboardScreen(" in line:
        started = True
        start_line = i + 1
    
    if started:
        for char in line:
            if char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
        
        if depth == 0 and '{' in line: 
            pass # wait until depth becomes 0 again
        if depth == 0 and i > start_line + 5:
            print(f"AutoAcceptDashboardScreen closes at line {i+1}")
            break

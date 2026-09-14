import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Remove SuccessStreakHeaderCard function
pattern_header = r'''@Composable\nfun SuccessStreakHeaderCard\([^)]*\)\s*\{[^{}]*\{[^{}]*\{[^{}]*\}[^{}]*\}[^{}]*\{[^{}]*\}[^{}]*\}'''
# It's a bit hard to match nested braces with regex. A simpler way is to find the function and remove it using string manipulation.

def remove_function(code, func_name):
    start_idx = code.find(f"fun {func_name}(")
    if start_idx == -1:
        return code
        
    # Find the preceding @Composable if it exists
    comp_idx = code.rfind("@Composable", 0, start_idx)
    if comp_idx != -1 and (start_idx - comp_idx) < 30:
        start_idx = comp_idx
        
    brace_count = 0
    in_function = False
    
    for i in range(start_idx, len(code)):
        if code[i] == '{':
            in_function = True
            brace_count += 1
        elif code[i] == '}':
            brace_count -= 1
            if in_function and brace_count == 0:
                return code[:start_idx] + code[i+1:]
                
    return code

content = remove_function(content, "SuccessStreakHeaderCard")
content = remove_function(content, "SuccessStreakTopBarBadge")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Removed Composable functions")

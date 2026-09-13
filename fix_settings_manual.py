with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

in_settings = False
for i, line in enumerate(lines):
    if "fun SettingsTabContent(" in line:
        in_settings = True
    
    if not in_settings:
        continue

    # Fix maxDistanceInput
    if "maxDistanceInput = filtered" in line:
        lines[i] = line.replace("maxDistanceInput = filtered", "onMaxDistanceChange(filtered)")
    if "AutoAcceptService.setMaxDistanceKm(context, dist)" in line:
        lines[i] = "// removed AutoAcceptService.setMaxDistanceKm"
    if "maxDistanceInput = preset" in line:
        lines[i] = line.replace("maxDistanceInput = preset", "onMaxDistanceChange(preset)")
    if "AutoAcceptService.setMaxDistanceKm(context, preset.toFloat())" in line:
        lines[i] = "// removed AutoAcceptService.setMaxDistanceKm"
        
    # Fix price filter
    if "minPriceInput = filtered" in line:
        lines[i] = line.replace("minPriceInput = filtered", "onMinPriceChange(filtered)")
    if "AutoAcceptService.setMinPrice(context, p)" in line:
        lines[i] = "// removed AutoAcceptService.setMinPrice"
        
    if "maxPriceInput = filtered" in line:
        lines[i] = line.replace("maxPriceInput = filtered", "onMaxPriceChange(filtered)")
    if "AutoAcceptService.setMaxPrice(context, p)" in line:
        lines[i] = "// removed AutoAcceptService.setMaxPrice"
        
    if "minPriceInput = minVal.toInt().toString()" in line:
        lines[i] = line.replace("minPriceInput = minVal.toInt().toString()", "onMinPriceChange(minVal.toInt().toString())")
    if "AutoAcceptService.setMinPrice(context, minVal)" in line:
        lines[i] = "// removed AutoAcceptService.setMinPrice"

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.writelines(lines)
print("Manual fix done!")

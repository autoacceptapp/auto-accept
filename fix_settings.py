import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Split the content to only replace within SettingsTabContent
parts = content.split("fun SettingsTabContent(")
if len(parts) == 2:
    header = parts[0]
    body = parts[1]
    
    # Distance Filter
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*isDistanceFilterOn\s*=\s*checked\s*AutoAcceptService\.setDistanceFilterEnabled\(context,\s*checked\)\s*\}", "onDistanceFilterChange(checked)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*val filtered[^}]*maxDistanceInput\s*=\s*filtered[^}]*AutoAcceptService\.setMaxDistanceKm[^}]*\}", "onMaxDistanceChange(newValue)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*&&\s*isDistanceFilterOn\s*\)\s*\{\s*maxDistanceInput\s*=\s*preset\s*AutoAcceptService\.setMaxDistanceKm[^}]*\}", "onMaxDistanceChange(preset)", body)

    # Price Filter
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*isPriceFilterOn\s*=\s*checked\s*AutoAcceptService\.setPriceFilterEnabled\(context,\s*checked\)\s*\}", "onPriceFilterChange(checked)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*val filtered[^}]*minPriceInput\s*=\s*filtered[^}]*AutoAcceptService\.setMinPrice[^}]*\}", "onMinPriceChange(newValue)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*val filtered[^}]*maxPriceInput\s*=\s*filtered[^}]*AutoAcceptService\.setMaxPrice[^}]*\}", "onMaxPriceChange(newValue)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*&&\s*isPriceFilterOn\s*\)\s*\{\s*minPriceInput\s*=\s*minVal\.toInt\(\)\.toString\(\)\s*AutoAcceptService\.setMinPrice[^}]*\}", "onMinPriceChange(minVal.toInt().toString())", body)

    # Blacklist Filter
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*isBlacklistFilterOn\s*=\s*checked\s*AutoAcceptService\.setBlacklistEnabled\(context,\s*checked\)\s*\}", "onBlacklistFilterChange(checked)", body)
    body = re.sub(r"if\s*\(\s*isPassActive\s*\)\s*\{\s*blacklistInput\s*=\s*newValue\s*AutoAcceptService\.setBlacklistKeywords[^}]*\}", "onBlacklistChange(newValue)", body)

    content = header + "fun SettingsTabContent(" + body
    
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)
    print("Fixed variable scopes!")
else:
    print("Failed to split.")

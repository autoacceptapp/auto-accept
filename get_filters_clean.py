import re

with open("/tmp/main_extract.kt", "r") as f:
    old_content = f.read()

dist_match = re.search(r"(// =========================================================================\s*// 2\. DISTANCE FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 3\. PRICE / FARE RANGE", old_content)
dist_card = dist_match.group(1) if dist_match else ""

price_match = re.search(r"(// =========================================================================\s*// 3\. PRICE / FARE RANGE FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 4\. BLACKLIST", old_content)
price_card = price_match.group(1) if price_match else ""

black_match = re.search(r"(// =========================================================================\s*// 4\. BLACKLIST KEYWORDS FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 5\. REAL-TIME", old_content)
black_card = black_match.group(1) if black_match else ""

def clean_card(card):
    # Fix the toggle changes
    card = re.sub(r"isDistanceFilterOn\s*=\s*checked\s+AutoAcceptService\.setDistanceFilterEnabled\(context,\s*checked\)", "onDistanceFilterChange(checked)", card)
    card = re.sub(r"isPriceFilterOn\s*=\s*checked\s+AutoAcceptService\.setPriceFilterEnabled\(context,\s*checked\)", "onPriceFilterChange(checked)", card)
    card = re.sub(r"isBlacklistFilterOn\s*=\s*checked\s+AutoAcceptService\.setBlacklistEnabled\(context,\s*checked\)", "onBlacklistFilterChange(checked)", card)

    # Fix the text field inputs
    card = re.sub(r"val filtered = newValue\.filter \{[^\}]+\}\s+maxDistanceInput = filtered\s+filtered\.toFloatOrNull\(\)\?\.let \{[^\}]+\}", "onMaxDistanceChange(newValue)", card)
    card = re.sub(r"val filtered = newValue\.filter \{[^\}]+\}\s+minPriceInput = filtered\s+filtered\.toFloatOrNull\(\)\?\.let \{[^\}]+\}", "onMinPriceChange(newValue)", card)
    card = re.sub(r"val filtered = newValue\.filter \{[^\}]+\}\s+maxPriceInput = filtered\s+filtered\.toFloatOrNull\(\)\?\.let \{[^\}]+\}", "onMaxPriceChange(newValue)", card)
    
    # Fix the preset chips
    card = re.sub(r"maxDistanceInput = preset\s+AutoAcceptService\.setMaxDistanceKm\(context,\s*preset\.toFloat\(\)\)", "onMaxDistanceChange(preset)", card)
    card = re.sub(r"minPriceInput = minVal\.toInt\(\)\.toString\(\)\s+AutoAcceptService\.setMinPrice\(context,\s*minVal\)", "onMinPriceChange(minVal.toInt().toString())", card)

    # Fix the blacklist input
    card = re.sub(r"blacklistInput = newValue\s+AutoAcceptService\.setBlacklistKeywords\(context,\s*newValue\)", "onBlacklistChange(newValue)", card)

    return card

dist_card = clean_card(dist_card)
price_card = clean_card(price_card)
black_card = clean_card(black_card)

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    main_content = f.read()

# I need to find where I injected them previously, and replace the WHOLE chunk with the clean chunk
# Since I injected them between `HorizontalDivider` and `// Accept Wait Delay Section` but closed the card.
# I will use a regex to replace everything between the end of Auto Accept Service toggle card and the Wait Delay Card
start_marker = r"// =========================================================================\n\s*// 2\. DISTANCE FILTER CARD \(PREMIUM FEATURE\)"
end_marker = r"// =========================================================================\n\s*// WAIT DELAY CONFIGURATION"

match = re.search(start_marker + r"[\s\S]*?" + end_marker, main_content)
if match:
    new_chunk = f"{dist_card}\n        {price_card}\n        {black_card}\n\n        // =========================================================================\n        // WAIT DELAY CONFIGURATION"
    main_content = main_content.replace(match.group(0), new_chunk)
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(main_content)
    print("Clean injection done!")
else:
    print("Could not find the injected chunk.")


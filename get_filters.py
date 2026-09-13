import re

with open("/tmp/main_extract.kt", "r") as f:
    old_content = f.read()

# Extract Distance Filter
dist_match = re.search(r"(// =========================================================================\s*// 2\. DISTANCE FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 3\. PRICE / FARE RANGE", old_content)
dist_card = dist_match.group(1) if dist_match else ""

price_match = re.search(r"(// =========================================================================\s*// 3\. PRICE / FARE RANGE FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 4\. BLACKLIST", old_content)
price_card = price_match.group(1) if price_match else ""

black_match = re.search(r"(// =========================================================================\s*// 4\. BLACKLIST KEYWORDS FILTER CARD \(PREMIUM FEATURE\)[\s\S]*?)// =========================================================================\s*// 5\. REAL-TIME", old_content)
black_card = black_match.group(1) if black_match else ""

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    main_content = f.read()

# Locate HorizontalDivider in the auto accept card
target = """                HorizontalDivider(color = Slate800, thickness = 1.dp)

                // Accept Wait Delay Section"""

new_divider_and_cards = f"""            }}
        }}

        {dist_card}
        {price_card}
        {black_card}

        // =========================================================================
        // WAIT DELAY CONFIGURATION
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wait_delay_settings_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {{
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {{
                // Accept Wait Delay Section"""

if target in main_content:
    main_content = main_content.replace(target, new_divider_and_cards)
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(main_content)
    print("Filters injected successfully!")
else:
    print("Could not find the target string.")


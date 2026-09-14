import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add a search query state to SettingsTabContent
search_state_code = """    var isCheckingUpdates by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }"""
content = re.sub(
    r'    var isCheckingUpdates by remember \{ mutableStateOf\(false\) \}',
    search_state_code,
    content
)

# Insert the search bar at the top of the Column
search_bar_code = """    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search settings or blacklist...", color = Slate400, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Slate400
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Slate400
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_search_bar"),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Slate800,
                focusedTextColor = Slate50,
                unfocusedTextColor = Slate300
            ),
            singleLine = true
        )
"""
content = re.sub(
    r'    Column\(\n        modifier = modifier\n            \.fillMaxSize\(\)\n            \.padding\(horizontal = 16\.dp, vertical = 12\.dp\)\n            \.verticalScroll\(rememberScrollState\(\)\)\n            \.testTag\("settings_screen"\),\n        verticalArrangement = Arrangement\.spacedBy\(16\.dp\)\n    \) \{',
    search_bar_code,
    content
)

# Apply filter to distance filter card
distance_filter_code = """        // =========================================================================
        SettingsSectionHeader("SMART FILTERS (PREMIUM)")
        
        val q = searchQuery.lowercase()
        
        if (q.isEmpty() || "distance".contains(q) || "max pickup".contains(q) || "km".contains(q)) {
            // 2. DISTANCE FILTER CARD (PREMIUM FEATURE)"""
content = re.sub(
    r'        // =========================================================================\n        SettingsSectionHeader\("SMART FILTERS \(PREMIUM\)"\)\n            // 2\. DISTANCE FILTER CARD \(PREMIUM FEATURE\)',
    distance_filter_code,
    content
)

# Apply filter to price filter card
price_filter_code = """            }
        }
        
        if (q.isEmpty() || "price".contains(q) || "fare".contains(q) || "min".contains(q) || "max".contains(q) || "₹".contains(q)) {
            // 3. PRICE & FARE FILTER CARD (PREMIUM FEATURE)"""
content = re.sub(
    r'            \}\n        \}\n\n            // 3\. PRICE & FARE FILTER CARD \(PREMIUM FEATURE\)',
    price_filter_code,
    content
)

# Apply filter to blacklist card
blacklist_code = """            }
        }
        
        if (q.isEmpty() || "blacklist".contains(q) || "location".contains(q) || "drop".contains(q) || "ignore".contains(q) || blacklistInput.lowercase().contains(q)) {
            // 4. BLACKLIST LOCATIONS FILTER (PREMIUM FEATURE)"""
content = re.sub(
    r'            \}\n        \}\n\n            // 4\. BLACKLIST LOCATIONS FILTER \(PREMIUM FEATURE\)',
    blacklist_code,
    content
)

# Apply filter to auto-accept config
auto_accept_code = """            }
        }
        
        if (q.isEmpty() || "advanced".contains(q) || "delay".contains(q) || "strict".contains(q) || "auto".contains(q) || "accept".contains(q)) {
        // =========================================================================
        SettingsSectionHeader("ADVANCED CONFIGURATION")"""
content = re.sub(
    r'            \}\n        \}\n\n        // =========================================================================\n        SettingsSectionHeader\("ADVANCED CONFIGURATION"\)',
    auto_accept_code,
    content
)

# Close the auto-accept if block
end_of_auto_accept_code = """                }
            }
        }
        } // End of advanced config if block

        // =========================================================================
        SettingsSectionHeader("ABOUT & UPDATES", showDivider = false)"""
content = re.sub(
    r'                \}\n            \}\n        \}\n\n        // =========================================================================\n        SettingsSectionHeader\("ABOUT & UPDATES", showDivider = false\)',
    end_of_auto_accept_code,
    content
)


with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

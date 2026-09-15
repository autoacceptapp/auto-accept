import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

help_card = """
        // =========================================================================
        // HELP & SUPPORT CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenHelpSupport() }
                .padding(vertical = 8.dp)
                .testTag("help_support_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Emerald400.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Help & Support",
                        tint = Emerald400,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Help & Support",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "FAQs, Troubleshooting & Automatic Guide",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Slate500
                )
            }
        }
"""

# Insert it before the end of the SettingsTabContent
# Since I'm not entirely sure where the exact end is, I'll place it right after the Advanced Settings card.
# The Advanced Settings card has a closing brace `}` followed by `}` and then `}`...
# Instead, let's just insert it before line 5403 by replacing `    }\n}\n\n\n}`? Let's check how many `}` there are.

# Let's search for "Check for Updates" button's parent Card end, and put it after it.
# It might be easier to just place it right above `SettingsSectionHeader("ADVANCED & UPDATES")`?
# The prompt says: "add a new card/button under the "ADVANCED & UPDATES" section"

# Let's replace the ending of the file, assuming it's the last thing in the scrollable column.
# Let's look for "Add new keyword..." block end.
add_keyword_block = """                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }"""

if add_keyword_block in content:
    content = content.replace(add_keyword_block, add_keyword_block + "\n" + help_card)
else:
    print("Could not find the keyword block.")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

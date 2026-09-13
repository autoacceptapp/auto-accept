import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Max Pickup Distance
distance_text = """
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Max Pickup Distance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate50
                                    )
                                    IconButton(onClick = { showDistanceInfo = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Cyan400, modifier = Modifier.size(16.dp))
                                    }
                                }"""
content = re.sub(r'Text\(\s*text\s*=\s*"Max Pickup Distance",\s*style\s*=\s*MaterialTheme\.typography\.titleMedium,\s*fontWeight\s*=\s*FontWeight\.Bold,\s*color\s*=\s*Slate50\s*\)', distance_text.strip(), content)

# Fare Range Filter
fare_text = """
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Fare Range Filter",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate50
                                    )
                                    IconButton(onClick = { showPriceInfo = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Cyan400, modifier = Modifier.size(16.dp))
                                    }
                                }"""
content = re.sub(r'Text\(\s*text\s*=\s*"Fare Range Filter",\s*style\s*=\s*MaterialTheme\.typography\.titleMedium,\s*fontWeight\s*=\s*FontWeight\.Bold,\s*color\s*=\s*Slate50\s*\)', fare_text.strip(), content)

# Strict Rapido Filter
rapido_text = """
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Strict Rapido Filter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate50
                            )
                            IconButton(onClick = { showRapidoInfo = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Cyan400, modifier = Modifier.size(16.dp))
                            }
                        }"""
content = re.sub(r'Text\(\s*text\s*=\s*"Strict Rapido Filter",\s*style\s*=\s*MaterialTheme\.typography\.bodyMedium,\s*fontWeight\s*=\s*FontWeight\.SemiBold,\s*color\s*=\s*Slate50\s*\)', rapido_text.strip(), content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

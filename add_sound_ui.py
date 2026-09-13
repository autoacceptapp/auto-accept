import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

sound_ui = """            }
        }

        // =========================================================================
        // CUSTOM NOTIFICATION SOUND
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_sound_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Accept Sound",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate50
                        )
                        Text(
                            text = if (customSoundUri.isNullOrEmpty()) "Default System Sound" else "Custom Ringtone Selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (customSoundUri.isNullOrEmpty()) Slate400 else Emerald400
                        )
                    }
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                customSoundUri?.let {
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(it))
                                }
                            }
                            soundPickerLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate200),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Select", fontSize = 12.sp)
                    }
                }
"""

content = content.replace("""                    Text("Test Voice Announcement", fontWeight = FontWeight.Bold)
                }
            }
        }""", """                    Text("Test Voice Announcement", fontWeight = FontWeight.Bold)
                }""" + sound_ui)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

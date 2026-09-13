with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will find the CUSTOM NOTIFICATION SOUND card and insert the closing braces.
target = """                    Button(
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
                }"""

replacement = target + """
            }
        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

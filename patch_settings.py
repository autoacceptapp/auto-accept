import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

autostart_card = """
                // 3. Auto-Start Permission
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                            androidx.compose.material3.Text(
                                text = "Auto-Start Permission",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = Slate100
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                            androidx.compose.material3.Text(
                                text = "Required for Xiaomi, HyperOS, Vivo, and Oppo devices to prevent OS battery killers from terminating the service",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent().apply {
                                        component = android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val oppoIntent = android.content.Intent().apply {
                                            component = android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(oppoIntent)
                                    } catch (e2: Exception) {
                                        val fallback = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(fallback)
                                    }
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Slate800,
                                contentColor = Slate300
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            androidx.compose.material3.Text("Settings", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
"""

# The Battery Optimization block ends at line 4236: } in the previous search
battery_end_pattern = r'(\s*Text\(if \(isBatteryOptimizationIgnored\) "Settings" else "Fix", fontSize = 12\.sp, fontWeight = FontWeight\.Bold\)\n\s*\}\n\s*\})'
match = re.search(battery_end_pattern, content)
if match:
    content = content[:match.end()] + "\n" + autostart_card + content[match.end():]
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched MainActivity.kt")
else:
    print("Could not find insertion point!")

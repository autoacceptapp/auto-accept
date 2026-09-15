import re

with open("app/src/main/java/com/example/PermissionsHelper.kt", "r") as f:
    content = f.read()

# Add imports if they are not there
if "import androidx.compose.material3.AlertDialog" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Warning",
                              "import androidx.compose.material.icons.filled.Warning\nimport androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.ButtonDefaults\nimport androidx.compose.material3.OutlinedButton\nimport androidx.compose.material3.Text\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.material3.Surface\nimport androidx.compose.material3.Icon")

# Add openAppInfoSettings and RestrictedSettingsGuideDialog
new_functions = """
fun openAppInfoSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open App Info", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RestrictedSettingsGuideDialog(
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A), // Slate900
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Android 13+ Security", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Android hides Accessibility settings for downloaded apps. If you see a 'Restricted Setting' warning, follow these steps:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF020617), border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Tap 'Open App Info' below.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("2. Tap the 3 dots (⋮) in the top right corner.", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("3. Tap 'Allow restricted settings' and enter your PIN/Fingerprint.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("4. Come back and enable Accessibility normally.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenAppInfo, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4), contentColor = Color(0xFF020617)), shape = RoundedCornerShape(10.dp)) {
                Text("1. Open App Info", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOpenAccessibility, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF334155)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))) {
                Text("2. Open Accessibility")
            }
        }
    )
}

"""

if "fun openAppInfoSettings(" not in content:
    content += new_functions

with open("app/src/main/java/com/example/PermissionsHelper.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/PermissionsHelper.kt", "r") as f:
    content = f.read()

new_dialogs = """
@Composable
fun NotificationAccessGuideDialog(onDismiss: () -> Unit, onProceed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = { Text("Notification Access", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("To detect incoming rides instantly, the app needs to read Rapido notifications.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF020617)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("1. Tap 'Proceed' below.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("2. Find and tap on 'Auto Accept'.", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("3. Turn ON 'Allow notification access'.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onProceed) { Text("Proceed") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun AutoStartGuideDialog(onDismiss: () -> Unit, onProceed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = { Text("Background Auto-Start", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("Prevent your phone from killing the app in the background while you drive.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF020617)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("1. Tap 'Proceed' below.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("2. Find 'Auto Accept' in the list.", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("3. Turn the switch ON.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onProceed) { Text("Proceed") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun DisplayOverlayGuideDialog(onDismiss: () -> Unit, onProceed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = { Text("Display Over Other Apps", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("Required to show the green auto-accept status bubble while you use maps or other apps.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF020617)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("1. Tap 'Proceed' below.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("2. Find and tap 'Auto Accept'.", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("3. Turn ON 'Allow display over other apps'.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onProceed) { Text("Proceed") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

"""

if "fun NotificationAccessGuideDialog" not in content:
    content += new_dialogs

# Add TextButton import if missing
if "import androidx.compose.material3.TextButton" not in content:
    content = content.replace("import androidx.compose.material3.Text", "import androidx.compose.material3.Text\nimport androidx.compose.material3.TextButton")

with open("app/src/main/java/com/example/PermissionsHelper.kt", "w") as f:
    f.write(content)


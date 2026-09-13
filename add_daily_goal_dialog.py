import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

state_vars = """
    var dailyGoal by remember { mutableStateOf(AutoAcceptService.getDailyGoal(context)) }
    var showGoalEditDialog by remember { mutableStateOf(false) }
    var goalInputText by remember { mutableStateOf(dailyGoal.toString()) }
"""
content = content.replace("    var dailyGoal by remember { mutableStateOf(AutoAcceptService.getDailyGoal(context)) }", state_vars.strip())

dialog_composable = """
    if (showGoalEditDialog) {
        AlertDialog(
            onDismissRequest = { showGoalEditDialog = false },
            containerColor = Slate900,
            title = {
                Text("Set Daily Goal", color = Slate50, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("How many rides do you want to accept today?", color = Slate300, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalInputText,
                        onValueChange = { goalInputText = it.filter { char -> char.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate50,
                            unfocusedTextColor = Slate300,
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newGoal = goalInputText.toIntOrNull() ?: 10
                        dailyGoal = if (newGoal < 1) 1 else newGoal
                        AutoAcceptService.setDailyGoal(context, dailyGoal)
                        showGoalEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalEditDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
"""

content = content.replace("    // Scaffold definition", dialog_composable.strip() + "\n\n    // Scaffold definition")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

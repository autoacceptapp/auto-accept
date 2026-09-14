import re

with open('app/src/main/java/com/example/DebugLogsScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove Simulate button from Empty State
simulate_button_pattern = r'''\s*Button\(\n\s*onClick = \{\n\s*simulateTestLog\(context\)\n\s*\},\n\s*colors = ButtonDefaults\.buttonColors\(\n\s*containerColor = Emerald600\n\s*\),\n\s*shape = RoundedCornerShape\(12\.dp\)\n\s*\) \{\n\s*Icon\(\n\s*imageVector = Icons\.Default\.PlayArrow,\n\s*contentDescription = "Simulate",\n\s*tint = Color\.White\n\s*\)\n\s*Spacer\(modifier = Modifier\.width\(8\.dp\)\)\n\s*Text\(\n\s*text = "Add Test Log",\n\s*color = Color\.White,\n\s*fontWeight = FontWeight\.SemiBold\n\s*\)\n\s*\}\n'''
content = re.sub(simulate_button_pattern, '', content)
print("Removed Add Test Log Button")

# 2. Remove "Add Test Log" button logic in EmptyState Component
empty_state_btn = r'''\s*Button\(\n\s*onClick = \{\n\s*simulateTestLog\(context\)\n\s*\},\n\s*colors = ButtonDefaults\.buttonColors\(\n\s*containerColor = Emerald600\n\s*\),\n\s*modifier = Modifier\.padding\(top = 16\.dp\)\n\s*\) \{\n\s*Text\("Add Test Log", color = Color\.White\)\n\s*\}\n'''
content = re.sub(empty_state_btn, '', content)

# 3. Remove "Simulate" Button from Secondary Action Bar
simulate_bar_btn = r'''\s*// Simulate Event Button\n\s*Surface\(\n\s*onClick = \{\n\s*simulateTestLog\(context\)\n\s*\},\n\s*shape = RoundedCornerShape\(10\.dp\),\n\s*color = Emerald500\.copy\(alpha = 0\.15f\),\n\s*border = BorderStroke\(1\.dp, Emerald500\.copy\(alpha = 0\.4f\)\),\n\s*modifier = Modifier\.testTag\("btn_simulate_diagnostic_event"\)\n\s*\) \{\n\s*Row\(\n\s*verticalAlignment = Alignment\.CenterVertically,\n\s*modifier = Modifier\.padding\(horizontal = 12\.dp, vertical = 10\.dp\)\n\s*\) \{\n\s*Icon\(\n\s*imageVector = Icons\.Default\.PlayArrow,\n\s*contentDescription = "Simulate",\n\s*tint = Emerald400,\n\s*modifier = Modifier\.size\(16\.dp\)\n\s*\)\n\s*Spacer\(modifier = Modifier\.width\(6\.dp\)\)\n\s*Text\(\n\s*text = "Simulate",\n\s*style = MaterialTheme\.typography\.labelMedium,\n\s*fontWeight = FontWeight\.Bold,\n\s*color = Emerald400\n\s*\)\n\s*\}\n\s*\}\n'''
content = re.sub(simulate_bar_btn, '', content)

with open('app/src/main/java/com/example/DebugLogsScreen.kt', 'w') as f:
    f.write(content)
print("Patched DebugLogsScreen.kt")

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Remove TopAppBar Button
icon_btn_pattern = r"""                    IconButton\(
                        onClick = \{ showPreferencesScreen = true \},
                        modifier = Modifier\.testTag\("topbar_preferences_button"\)
                    \) \{
                        Icon\(
                            imageVector = Icons\.Default\.Tune,
                            contentDescription = "Ride Preferences",
                            tint = Cyan400
                        \)
                    \}"""
content = re.sub(icon_btn_pattern, "", content)

# 2. Remove Navigation Block
nav_block_pattern = r"""    if \(showPreferencesScreen\) \{
        BackHandler \{
            showPreferencesScreen = false
        \}
        com\.example\.ui\.PreferencesScreen\(
            onNavigateBack = \{ showPreferencesScreen = false \}
        \)
        return
    \}"""
content = re.sub(nav_block_pattern, "", content)

# 3. Remove State Variable
state_var_pattern = r"""    var showPreferencesScreen by remember \{ mutableStateOf\(false\) \}
"""
content = re.sub(state_var_pattern, "", content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n@Composable\nfun SettingsSectionHeader", "@Composable\nfun SettingsSectionHeader")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

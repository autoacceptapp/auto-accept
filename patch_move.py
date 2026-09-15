import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove the old declaration
content = content.replace("    var showRestrictedGuide by remember { mutableStateOf(false) }\n", "")

# Insert it around line 500 (just after fun AutoAcceptDashboardScreen)
new_decl = "    var showRestrictedGuide by remember { mutableStateOf(false) }\n"
if "fun AutoAcceptDashboardScreen(" in content:
    content = content.replace("fun AutoAcceptDashboardScreen(\n    currentUser: FirebaseUser?,", 
                              "fun AutoAcceptDashboardScreen(\n    currentUser: FirebaseUser?,\n" + 
                              "    onSignOut: () -> Unit,\n" +
                              "    onSignInClick: () -> Unit\n" +
                              ") {\n" + new_decl)

    # Note: I need to be careful with exact string replacement.

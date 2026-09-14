import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Remove showSplash state from AutoAcceptDashboardScreen
#     var showSplash by rememberSaveable { mutableStateOf(true) }
#
#     if (showSplash) {
#         SplashScreen(onTimeout = { showSplash = false })
#         return
#     }
content = re.sub(
    r'\s*var showSplash by rememberSaveable \{ mutableStateOf\(true\) \}\s*\n\s*if \(showSplash\) \{\s*\n\s*SplashScreen\(onTimeout = \{ showSplash = false \}\)\s*\n\s*return\s*\n\s*\}',
    '',
    content
)

# 2. Remove the SplashScreen composable entirely
# Search for @Composable fun SplashScreen(onTimeout: () -> Unit) { ... }
# Since it goes up to line 2744 (the brace before VisualLogViewCard)
content = re.sub(
    r'@Composable\s*\nfun SplashScreen\(onTimeout: \(\) -> Unit\) \{.*?^\}',
    '',
    content,
    flags=re.MULTILINE | re.DOTALL
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


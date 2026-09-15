import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Inject the package check before executeAcceptClick
check_old = """                    // BUG 10: Change SUCCESS logging!
                    when (val outcome = executeAcceptClick(revalidatedButton!!)) {"""
check_new = """                    val executeRoot = try { rootInActiveWindow } catch(e: Exception) { null }
                    if (executeRoot == null || executeRoot.packageName?.toString() != sourcePackage) {
                        Log.w(TAG, "ACCEPT_REVALIDATION_FAILED: PACKAGE_MISMATCH (Right before click)")
                        _recentLog.value = "Accept cancelled: PACKAGE_MISMATCH"
                        return@launch
                    }

                    // BUG 10: Change SUCCESS logging!
                    when (val outcome = executeAcceptClick(revalidatedButton!!)) {"""

content = content.replace(check_old, check_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

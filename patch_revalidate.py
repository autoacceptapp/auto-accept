import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

reval_old = """                    val newRoot = try { rootInActiveWindow } catch(e: Exception) { null }
                    if (newRoot == null) {
                        Log.w(TAG, "ACCEPT_REVALIDATION_FAILED: rootInActiveWindow unavailable")
                        return@launch
                    }

                    // Re-discover accept buttons and extract data to verify signature hasn't changed"""
reval_new = """                    val newRoot = try { rootInActiveWindow } catch(e: Exception) { null }
                    if (newRoot == null) {
                        Log.w(TAG, "ACCEPT_REVALIDATION_FAILED: rootInActiveWindow unavailable")
                        return@launch
                    }
                    
                    val newPackage = newRoot.packageName?.toString()
                    if (newPackage == null || newPackage != sourcePackage) {
                        Log.w(TAG, "ACCEPT_REVALIDATION_FAILED: PACKAGE_MISMATCH (Expected $sourcePackage, got $newPackage)")
                        _recentLog.value = "Accept cancelled: PACKAGE_MISMATCH"
                        return@launch
                    }

                    // Re-discover accept buttons and extract data to verify signature hasn't changed"""
content = content.replace(reval_old, reval_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

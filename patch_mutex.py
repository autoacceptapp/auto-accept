import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# Add Mutex import if missing
if "import kotlinx.coroutines.sync.Mutex" not in content:
    content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.sync.Mutex\nimport kotlinx.coroutines.sync.withLock")

# Add acceptMutex to companion object or class properties
mutex_prop = "    private val acceptMutex = Mutex()"
if mutex_prop not in content:
    content = content.replace("private val wakeLockMutex = Any()", "private val wakeLockMutex = Any()\n" + mutex_prop)

# Wrap the final check and click in acceptMutex.withLock
reval_check_old = """                    val recentAcceptedCheck = recentlyAcceptedRides[capturedRide.signature]
                    if (recentAcceptedCheck != null && System.currentTimeMillis() - recentAcceptedCheck < DUPLICATE_COOLDOWN_MS) {
                        Log.w(TAG, "Pending accept cancelled: duplicate cooldown active")
                        return@launch
                    }

                    if (isPremium && isVoiceOnlyMode(this@AutoAcceptService)) {"""

reval_check_new = """                    acceptMutex.withLock {
                        val recentAcceptedCheck = recentlyAcceptedRides[capturedRide.signature]
                        if (recentAcceptedCheck != null && System.currentTimeMillis() - recentAcceptedCheck < DUPLICATE_COOLDOWN_MS) {
                            Log.w(TAG, "Pending accept cancelled: duplicate cooldown active")
                            return@launch
                        }
    
                        if (isPremium && isVoiceOnlyMode(this@AutoAcceptService)) {"""

content = content.replace(reval_check_old, reval_check_new)

# Add closing brace for withLock
finally_old = """                        is ClickResult.Failed -> {
                            Log.e(TAG, "Click failed: ${outcome.reason}")
                        }
                    }
                } catch (e: Exception) {"""
finally_new = """                        is ClickResult.Failed -> {
                            Log.e(TAG, "Click failed: ${outcome.reason}")
                        }
                    }
                    } // end withLock
                } catch (e: Exception) {"""

content = content.replace(finally_old, finally_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_signin = """                        // Automatically initialize Driver Name if default
                        signedInUser?.displayName?.let { name ->
                            if (AutoAcceptService.getUserName(context) == AutoAcceptService.DEFAULT_USER_NAME) {
                                val firstName = name.split(" ").firstOrNull() ?: name
                                AutoAcceptService.setUserName(context, firstName)
                            }
                        }"""

new_signin = """                        // Automatically initialize Driver Name if default
                        signedInUser?.displayName?.let { name ->
                            if (AutoAcceptService.getUserName(context) == AutoAcceptService.DEFAULT_USER_NAME) {
                                val firstName = name.split(" ").firstOrNull() ?: name
                                AutoAcceptService.setUserName(context, firstName)
                            }
                        }
                        
                        // Award Welcome Points on First Device Registration
                        signedInUser?.uid?.let { uid ->
                            coroutineScope.launch {
                                com.example.SubscriptionManager.registerDeviceForWelcomePoints(uid, context)
                            }
                        }"""

if old_signin in content:
    content = content.replace(old_signin, new_signin)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

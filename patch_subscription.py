import re

with open("app/src/main/java/com/example/SubscriptionManager.kt", "r") as f:
    content = f.read()

# Replace the throw for claimed device in applyReferralCode
content = content.replace('throw IllegalStateException("This device has already claimed a referral bonus.")', 
                          'throw IllegalStateException("Is phone par referral code pehle se use ho chuka hai.")')

# Add the new device registration function before applyReferralCode
new_func = """    /**
     * New Device Registration (50 Points):
     * - Checks devices/$androidId document
     * - If not exists, creates it with 50 points and awards 50 points to the user.
     */
    suspend fun registerDeviceForWelcomePoints(userId: String, context: Context): Result<String> {
        return try {
            if (userId.isBlank()) return Result.failure(IllegalArgumentException("Invalid user ID."))
            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
            
            val firestore = try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                FirebaseHelper.initialize(context)
                FirebaseFirestore.getInstance()
            }
            
            val deviceRef = firestore.collection("devices").document(deviceId)
            val userRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(userId)

            var awarded = false

            firestore.runTransaction { transaction ->
                val deviceSnap = transaction.get(deviceRef)
                if (!deviceSnap.exists()) {
                    // Create device doc with 50 points
                    transaction.set(deviceRef, mapOf(
                        "points" to 50,
                        "registeredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "claimedByUid" to userId
                    ))

                    // Credit 50 Welcome Points to user
                    val userSnap = transaction.get(userRef)
                    val currentPoints = if (userSnap.exists()) {
                        userSnap.getLong("points")?.toInt() ?: 0
                    } else {
                        0
                    }
                    
                    transaction.set(
                        userRef,
                        mapOf(
                            "userId" to userId,
                            "points" to currentPoints + 50,
                            "referralCode" to getReferralCodeForUser(userId)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    awarded = true
                }
            }.await()
            
            if (awarded) {
                Result.success("50 Welcome Points awarded!")
            } else {
                Result.success("Device already registered.")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

"""

if "fun registerDeviceForWelcomePoints" not in content:
    content = content.replace("    suspend fun applyReferralCode(", new_func + "    suspend fun applyReferralCode(")


# Make sure the increment by 20 is used, instead of calculating and setting. 
# Wait, currently it's doing:
# val currentReferrerPoints = if (referrerSnapshot.exists()) ...
# val newReferrerPoints = currentReferrerPoints + REFERRAL_BONUS_REFERRER
# transaction.set(referrerRef, mapOf("points" to newReferrerPoints ...))
#
# The user asked: "transaction.update(referrerDocRef, "points", FieldValue.increment(20))"

# Let's fix the applyReferralCode completely.

with open("app/src/main/java/com/example/SubscriptionManager.kt", "w") as f:
    f.write(content)


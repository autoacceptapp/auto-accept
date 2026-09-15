import re

with open("app/src/main/java/com/example/SubscriptionManager.kt", "r") as f:
    content = f.read()

# Replace the transaction block in applyReferralCode
old_transaction = """            firestore.runTransaction { transaction ->
                val deviceSnapshot = transaction.get(deviceRef)
                if (deviceSnapshot.exists()) {
                    throw IllegalStateException("Is phone par referral code pehle se use ho chuka hai.")
                }

                val userSnapshot = transaction.get(userRef)
                if (userSnapshot.exists()) {
                    val existingReferrer = userSnapshot.getString("referredBy")
                    if (!existingReferrer.isNullOrBlank()) {
                        throw IllegalStateException("You have already applied a referral code!")
                    }
                }

                val referrerSnapshot = transaction.get(referrerRef)
                val currentReferrerPoints = if (referrerSnapshot.exists()) {
                    referrerSnapshot.getLong("points")?.toInt() ?: 0
                } else {
                    0
                }

                val currentUserPoints = if (userSnapshot.exists()) {
                    userSnapshot.getLong("points")?.toInt() ?: WELCOME_BONUS_POINTS
                } else {
                    WELCOME_BONUS_POINTS
                }

                // Award 20 Points to Referrer
                val newReferrerPoints = currentReferrerPoints + REFERRAL_BONUS_REFERRER
                transaction.set(
                    referrerRef,
                    mapOf(
                        "userId" to referrerDocId,
                        "points" to newReferrerPoints,
                        "referralCode" to getReferralCodeForUser(referrerDocId),
                        "totalReferrals" to ((referrerSnapshot.getLong("totalReferrals") ?: 0L) + 1L)
                    ),
                    SetOptions.merge()
                )

                // Award 50 Points to New User (in addition to welcome bonus)
                val newUserPoints = currentUserPoints + REFERRAL_BONUS_NEW_USER
                transaction.set(
                    userRef,
                    mapOf(
                        "userId" to currentUserId,
                        "points" to newUserPoints,
                        "referralCode" to getReferralCodeForUser(currentUserId),
                        "referredBy" to referrerDocId,
                        "referralCodeUsed" to cleanCode,
                        "referralAppliedAt" to now
                    ),
                    SetOptions.merge()
                )

                // Log the referral in user_referrals collection
                transaction.set(
                    referralLogRef,
                    mapOf(
                        "referrerUserId" to referrerDocId,
                        "referredUserId" to currentUserId,
                        "referralCode" to cleanCode,
                        "referrerBonusPoints" to REFERRAL_BONUS_REFERRER,
                        "newUserBonusPoints" to REFERRAL_BONUS_NEW_USER,
                        "timestamp" to now
                    )
                )
                
                transaction.set(
                    deviceRef,
                    hashMapOf(
                        "deviceId" to deviceId,
                        "claimedByUid" to currentUserId,
                        "referralCodeUsed" to cleanCode,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()"""

new_transaction = """            firestore.runTransaction { transaction ->
                val deviceSnapshot = transaction.get(deviceRef)
                if (deviceSnapshot.exists()) {
                    throw IllegalStateException("Is phone par referral code pehle se use ho chuka hai.")
                }

                val userSnapshot = transaction.get(userRef)
                if (userSnapshot.exists()) {
                    val existingReferrer = userSnapshot.getString("referredBy")
                    if (!existingReferrer.isNullOrBlank()) {
                        throw IllegalStateException("You have already applied a referral code!")
                    }
                }

                val referrerSnapshot = transaction.get(referrerRef)
                val referrerDeviceId = referrerSnapshot.getString("activeDeviceId")
                
                // Self-referral protection: Referrer ka device ID aur user ka device ID same nahi hona chahiye
                if (referrerDeviceId != null && referrerDeviceId == deviceId) {
                    throw IllegalStateException("You cannot refer your own device.")
                }

                // Award 20 Points to Referrer
                transaction.set(
                    referrerRef,
                    mapOf(
                        "userId" to referrerDocId,
                        "points" to com.google.firebase.firestore.FieldValue.increment(20L),
                        "referralCode" to getReferralCodeForUser(referrerDocId),
                        "totalReferrals" to com.google.firebase.firestore.FieldValue.increment(1L)
                    ),
                    SetOptions.merge()
                )

                // Award 50 Points to New User (in addition to welcome bonus)
                transaction.set(
                    userRef,
                    mapOf(
                        "userId" to currentUserId,
                        "points" to com.google.firebase.firestore.FieldValue.increment(50L),
                        "referralCode" to getReferralCodeForUser(currentUserId),
                        "referredBy" to referrerDocId,
                        "referralCodeUsed" to cleanCode,
                        "referralAppliedAt" to now
                    ),
                    SetOptions.merge()
                )

                // Log the referral in user_referrals collection
                transaction.set(
                    referralLogRef,
                    mapOf(
                        "referrerUserId" to referrerDocId,
                        "referredUserId" to currentUserId,
                        "referralCode" to cleanCode,
                        "referrerBonusPoints" to 20,
                        "newUserBonusPoints" to 50,
                        "timestamp" to now
                    )
                )
                
                transaction.set(
                    deviceRef,
                    hashMapOf(
                        "deviceId" to deviceId,
                        "claimedByUid" to currentUserId,
                        "referralCodeUsed" to cleanCode,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.await()"""

if old_transaction in content:
    content = content.replace(old_transaction, new_transaction)
else:
    print("Could not find old transaction to replace!")

with open("app/src/main/java/com/example/SubscriptionManager.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/SubscriptionManager.kt", "r") as f:
    content = f.read()

# 1. Update applyReferralCode method signature and initial checks
old_checks = """        return try {
            val cleanCode = referralCodeInput.trim().uppercase()
            if (cleanCode.isBlank()) {
                return Result.failure(IllegalArgumentException("Please enter a valid referral code."))
            }
            if (currentUserId.isBlank()) {
                return Result.failure(IllegalArgumentException("Please sign in with Google first to apply a referral code."))
            }

            val ownCode = getReferralCodeForUser(currentUserId)
            if (cleanCode == ownCode || cleanCode == currentUserId.uppercase()) {
                return Result.failure(IllegalArgumentException("You cannot use your own referral code."))
            }"""

new_checks = """        return try {
            val cleanCode = referralCodeInput.trim().uppercase()
            if (cleanCode.isBlank()) {
                return Result.failure(IllegalArgumentException("Please enter a valid referral code."))
            }
            if (currentUserId.isBlank()) {
                return Result.failure(IllegalArgumentException("Please sign in with Google first to apply a referral code."))
            }

            val ownCode = getReferralCodeForUser(currentUserId)
            if (cleanCode == ownCode || cleanCode == currentUserId.uppercase()) {
                return Result.failure(IllegalArgumentException("You cannot use your own referral code."))
            }

            if (context == null) {
                return Result.failure(IllegalArgumentException("Context is required for device verification."))
            }

            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
            val sharedPrefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
            val isClaimedLocally = sharedPrefs.getBoolean("KEY_REFERRAL_CLAIMED_ON_DEVICE", false)

            if (isClaimedLocally) {
                return Result.failure(IllegalStateException("This device has already claimed a referral bonus."))
            }"""

content = content.replace(old_checks, new_checks)

# 2. Add claimed_referral_devices reference and modify transaction
old_transaction = """            val userRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(currentUserId)
            val referrerRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(referrerDocId)
            val referralLogRef = firestore.collection(COLLECTION_REFERRALS).document("${referrerDocId}_$currentUserId")

            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                if (userSnapshot.exists()) {
                    val existingReferrer = userSnapshot.getString("referredBy")
                    if (!existingReferrer.isNullOrBlank()) {
                        throw IllegalStateException("You have already applied a referral code!")
                    }
                }"""

new_transaction = """            val userRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(currentUserId)
            val referrerRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(referrerDocId)
            val referralLogRef = firestore.collection(COLLECTION_REFERRALS).document("${referrerDocId}_$currentUserId")
            val deviceRef = firestore.collection("claimed_referral_devices").document(deviceId)

            firestore.runTransaction { transaction ->
                val deviceSnapshot = transaction.get(deviceRef)
                if (deviceSnapshot.exists()) {
                    throw IllegalStateException("This device has already claimed a referral bonus.")
                }

                val userSnapshot = transaction.get(userRef)
                if (userSnapshot.exists()) {
                    val existingReferrer = userSnapshot.getString("referredBy")
                    if (!existingReferrer.isNullOrBlank()) {
                        throw IllegalStateException("You have already applied a referral code!")
                    }
                }"""

content = content.replace(old_transaction, new_transaction)


# 3. Add claimed_referral_devices save to transaction
old_transaction_end = """                // Log the referral in user_referrals collection
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
            }.await()

            Log.i(TAG, "Referral applied successfully: $currentUserId referred by $referrerDocId (+20 to referrer, +50 to new user)")"""

new_transaction_end = """                // Log the referral in user_referrals collection
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
            }.await()
            
            sharedPrefs.edit().putBoolean("KEY_REFERRAL_CLAIMED_ON_DEVICE", true).apply()

            Log.i(TAG, "Referral applied successfully: $currentUserId referred by $referrerDocId (+20 to referrer, +50 to new user)")"""

content = content.replace(old_transaction_end, new_transaction_end)

with open("app/src/main/java/com/example/SubscriptionManager.kt", "w") as f:
    f.write(content)

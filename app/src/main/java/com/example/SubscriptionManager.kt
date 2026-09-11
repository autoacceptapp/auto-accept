package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeoutException

// =============================================================================
// SUB STATE: DUAL-LOCK SUBSCRIPTION, TRIAL STATUS & REWARD POINTS
// =============================================================================
sealed class SubState {
    abstract val points: Int

    object Loading : SubState() {
        override val points: Int = 0
    }
    data class PremiumActive(val planType: String, val expiryMillis: Long, override val points: Int = 0) : SubState()
    data class TrialActive(val hoursLeft: Long, val expiryMillis: Long, override val points: Int = 0) : SubState()
    open class TrialExpired(override val points: Int = 0) : SubState() {
        override fun equals(other: Any?): Boolean = other is TrialExpired && other.points == this.points
        override fun hashCode(): Int = points.hashCode()
        override fun toString(): String = "TrialExpired(points=$points)"
        companion object : TrialExpired(0)
    }
    open class NoSubscription(override val points: Int = 0) : SubState() {
        override fun equals(other: Any?): Boolean = other is NoSubscription && other.points == this.points
        override fun hashCode(): Int = points.hashCode()
        override fun toString(): String = "NoSubscription(points=$points)"
        companion object : NoSubscription(0)
    }
    data class DeviceMismatch(val activeDeviceId: String, override val points: Int = 0) : SubState()
}

object SubscriptionManager {

    private const val TAG = "SubscriptionManager"
    private const val COLLECTION_TRIALS = "device_trials"
    private const val COLLECTION_SUBSCRIPTIONS = "user_subscriptions"

    // 48 hours in milliseconds
    const val TRIAL_DURATION_MS = 48 * 3600 * 1000L

    // Reward Points Costs for Free Pass Redemption
    const val POINTS_DAILY_PASS = 100
    const val POINTS_WEEKLY_PASS = 500
    const val POINTS_MONTHLY_PASS = 1500
    const val WELCOME_BONUS_POINTS = 50
    const val REFERRAL_BONUS_REFERRER = 20
    const val REFERRAL_BONUS_NEW_USER = 50

    private const val COLLECTION_REFERRALS = "user_referrals"

    /**
     * Observes real-time subscription, free trial status, and gamified reward points balance.
     * Dual-Lock Security:
     * 1. Paid Pass & Points check on user_subscriptions/{uid} (Plan, Expiry, Active Device Lock, Points balance).
     * 2. Free Trial check on device_trials/{androidId} (48 hours auto-created on first launch).
     */
    fun observeSubscription(
        context: Context,
        androidId: String,
        userId: String?
    ): Flow<SubState> = callbackFlow {
        trySend(SubState.Loading)

        val firebaseApp = try {
            FirebaseHelper.initialize(context)
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization error: ${e.message}")
            null
        }

        if (firebaseApp == null) {
            trySend(SubState.NoSubscription(points = 0))
            close()
            return@callbackFlow
        }

        val firestore = try {
            FirebaseFirestore.getInstance(firebaseApp)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase unavailable: ${e.message}")
            null
        }

        if (firestore == null) {
            trySend(SubState.NoSubscription(points = 0))
            close()
            return@callbackFlow
        }

        var trialRegistration: ListenerRegistration? = null
        var subRegistration: ListenerRegistration? = null

        var currentTrialDoc: com.google.firebase.firestore.DocumentSnapshot? = null
        var currentSubDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        fun evaluateAndEmit() {
            val now = System.currentTimeMillis()

            // Determine user reward points
            var userPoints = 0
            if (!userId.isNullOrBlank()) {
                val subDoc = currentSubDoc
                if (subDoc != null) {
                    if (subDoc.exists()) {
                        val rawPoints = subDoc.getLong("points")
                        if (rawPoints != null) {
                            userPoints = rawPoints.toInt()
                        } else {
                            // Existing document without points initialized: award 50 welcome bonus points
                            userPoints = WELCOME_BONUS_POINTS
                            try {
                                firestore.collection(COLLECTION_SUBSCRIPTIONS)
                                    .document(userId)
                                    .update("points", WELCOME_BONUS_POINTS)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error setting welcome bonus points: ${e.message}")
                            }
                        }
                    } else {
                        // User document doesn't exist yet: initialize with 50 welcome bonus points
                        userPoints = WELCOME_BONUS_POINTS
                        try {
                            val newDoc = mapOf(
                                "userId" to userId,
                                "points" to WELCOME_BONUS_POINTS,
                                "createdAt" to now,
                                "welcomeBonusGiven" to true
                            )
                            firestore.collection(COLLECTION_SUBSCRIPTIONS)
                                .document(userId)
                                .set(newDoc, SetOptions.merge())
                        } catch (e: Exception) {
                            Log.w(TAG, "Error initializing user document with welcome bonus: ${e.message}")
                        }
                    }
                }
            }

            // 1. Evaluate Paid User Subscription first if user is signed in
            if (!userId.isNullOrBlank()) {
                val subDoc = currentSubDoc
                if (subDoc != null && subDoc.exists()) {
                    val expiryMillis = subDoc.getLong("expiryMillis")
                        ?: subDoc.getTimestamp("expiryTime")?.toDate()?.time
                        ?: 0L
                    val planType = subDoc.getString("planType") ?: "premium"
                    val activeDeviceId = subDoc.getString("activeDeviceId") ?: ""

                    if (expiryMillis > now) {
                        // Check Device Lock
                        if (activeDeviceId.isNotBlank() && activeDeviceId != androidId) {
                            Log.w(TAG, "Device mismatch: active on $activeDeviceId, current is $androidId")
                            trySend(SubState.DeviceMismatch(activeDeviceId = activeDeviceId, points = userPoints))
                            return
                        } else {
                            trySend(SubState.PremiumActive(planType = planType, expiryMillis = expiryMillis, points = userPoints))
                            return
                        }
                    }
                }
            }

            // 2. Evaluate Device Free Trial
            val trialDoc = currentTrialDoc
            if (trialDoc != null) {
                if (!trialDoc.exists()) {
                    // Trial document does not exist yet: create 48-hour free trial
                    val expiry = now + TRIAL_DURATION_MS
                    val newTrial = mapOf(
                        "deviceId" to androidId,
                        "createdAt" to now,
                        "expiryMillis" to expiry,
                        "durationHours" to 48
                    )
                    firestore.collection(COLLECTION_TRIALS)
                        .document(androidId)
                        .set(newTrial)
                        .addOnSuccessListener {
                            Log.d(TAG, "Created 48-hour trial for device $androidId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to create trial: ${e.message}")
                        }

                    trySend(SubState.TrialActive(hoursLeft = 48, expiryMillis = expiry, points = userPoints))
                    return
                } else {
                    val expiryMillis = trialDoc.getLong("expiryMillis")
                        ?: trialDoc.getTimestamp("expiryTime")?.toDate()?.time
                        ?: 0L

                    if (expiryMillis > now) {
                        val hoursLeft = ((expiryMillis - now) / (3600 * 1000L)).coerceAtLeast(1L)
                        trySend(SubState.TrialActive(hoursLeft = hoursLeft, expiryMillis = expiryMillis, points = userPoints))
                        return
                    } else {
                        trySend(SubState.TrialExpired(points = userPoints))
                        return
                    }
                }
            }

            trySend(SubState.Loading)
        }

        // Setup Trial Listener for deviceId
        trialRegistration = firestore.collection(COLLECTION_TRIALS)
            .document(androidId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Trial listener error: ${error.message}")
                    return@addSnapshotListener
                }
                currentTrialDoc = snapshot
                evaluateAndEmit()
            }

        // Setup Subscription Listener if user is signed in
        if (!userId.isNullOrBlank()) {
            subRegistration = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Subscription listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    currentSubDoc = snapshot
                    evaluateAndEmit()
                }
        } else {
            // Not signed in, evaluate with null subDoc
            currentSubDoc = null
            evaluateAndEmit()
        }

        awaitClose {
            trialRegistration.remove()
            subRegistration?.remove()
        }
    }

    /**
     * Transfers an active paid subscription to the current phone.
     * Updates activeDeviceId in user_subscriptions/{userId} to androidId.
     */
    suspend fun transferSubscription(
        userId: String,
        androidId: String,
        context: Context? = null
    ): Result<Unit> {
        return try {
            val firebaseApp = try {
                if (context != null) {
                    FirebaseHelper.initialize(context)
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                try { FirebaseApp.getInstance() } catch (ex: Exception) { null }
            }

            val firestore = try {
                if (firebaseApp != null) FirebaseFirestore.getInstance(firebaseApp) else FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore not initialized: ${e.message}")
                return Result.failure(e)
            }

            firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .document(userId)
                .update(
                    mapOf(
                        "activeDeviceId" to androidId,
                        "lastTransferredAt" to System.currentTimeMillis()
                    )
                ).await()
            Log.i(TAG, "Subscription transferred to device $androidId for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transfer subscription: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies the UPI UTR in received_payments/{utr} and activates the pass in user_subscriptions/{userId}
     * using an atomic Firestore Transaction to ensure strict double-spending protection.
     * Implements a long-running auto-polling mechanism checking every 2 seconds for up to 24 hours (43,200 iterations)
     * until the document has the correct amount or status == "Verified".
     *
     * Plans:
     * - "daily": 24 hours, ₹9
     * - "weekly": 7 days, ₹49
     * - "monthly": 30 days, ₹179
     */
    suspend fun verifyUtrAndActivatePass(
        userId: String,
        plan: String,
        androidId: String,
        utr: String,
        context: Context? = null
    ): Result<Unit> {
        return try {
            val cleanUtr = utr.trim()
            if (cleanUtr.isBlank()) {
                return Result.failure(IllegalArgumentException("Please enter a valid 12-digit UTR."))
            }

            val firebaseApp = try {
                if (context != null) {
                    FirebaseHelper.initialize(context)
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                try { FirebaseApp.getInstance() } catch (ex: Exception) { null }
            }

            val firestore = try {
                if (firebaseApp != null) FirebaseFirestore.getInstance(firebaseApp) else FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore not initialized: ${e.message}")
                return Result.failure(e)
            }

            val now = System.currentTimeMillis()

            val (durationMs, requiredPrice) = when (plan.lowercase()) {
                "daily" -> Pair(24 * 3600 * 1000L, 9)
                "weekly" -> Pair(7 * 24 * 3600 * 1000L, 49)
                "monthly" -> Pair(30 * 24 * 3600 * 1000L, 179)
                else -> Pair(24 * 3600 * 1000L, 9)
            }

            val paymentRef = firestore.collection("received_payments").document(cleanUtr)
            val subscriptionRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(userId)

            val maxIterations = 43200 // 43,200 iterations * 2 seconds = 86,400 seconds = 24 hours
            var verified = false

            // 1. Polling/Waiting Phase: Wait for the Firestore document's `status` to become "Verified" (updated by MacroDroid)
            for (attempt in 1..maxIterations) {
                try {
                    val paymentDoc = paymentRef.get().await()
                    if (paymentDoc.exists()) {
                        val isUsed = paymentDoc.getBoolean("isUsed") ?: false
                        if (isUsed) {
                            throw Exception("This UTR has already been claimed. Pass cannot be activated.")
                        }

                        val docStatus = paymentDoc.getString("status") ?: ""
                        val docAmount = when (val raw = paymentDoc.get("amount")) {
                            is Number -> raw.toInt()
                            is String -> raw.toDoubleOrNull()?.toInt() ?: -1
                            else -> -1
                        }

                        if (docStatus.equals("Verified", ignoreCase = true) ||
                            docStatus.equals("PAYMENT_VERIFIED", ignoreCase = true) ||
                            docAmount == requiredPrice
                        ) {
                            Log.i(TAG, "UTR $cleanUtr verified by backend on attempt $attempt (status=$docStatus, amount=$docAmount)")
                            verified = true
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (e.message == "This UTR has already been claimed. Pass cannot be activated.") {
                        throw e
                    }
                    Log.w(TAG, "Polling check attempt $attempt: ${e.message}")
                }

                if (attempt < maxIterations) {
                    delay(2000L)
                }
            }

            if (!verified) {
                throw TimeoutException("Payment verification timed out. Payment not marked as Verified yet. UTR: $cleanUtr")
            }

            // 2. The Secure Transaction Phase:
            // Once verified, execute a Firestore runTransaction:
            // - Read exact state of payment document.
            // - Check isUsed. If true, ABORT immediately with exact error.
            // - If false/null, explicitly set isUsed = true, usedByDeviceId, usedByUserId, usedAt.
            // - Then set user's subscription document to activate the pass.
            firestore.runTransaction { transaction ->
                val paymentDoc = transaction.get(paymentRef)
                if (!paymentDoc.exists()) {
                    throw Exception("Payment not found for UTR: $cleanUtr. If you just paid, please wait a moment and try again.")
                }

                val isUsed = paymentDoc.getBoolean("isUsed") ?: false
                if (isUsed) {
                    throw Exception("This UTR has already been claimed. Pass cannot be activated.")
                }

                val docStatus = paymentDoc.getString("status") ?: ""
                val docAmount = when (val raw = paymentDoc.get("amount")) {
                    is Number -> raw.toInt()
                    is String -> raw.toDoubleOrNull()?.toInt() ?: -1
                    else -> -1
                }

                if (!docStatus.equals("Verified", ignoreCase = true) &&
                    !docStatus.equals("PAYMENT_VERIFIED", ignoreCase = true) &&
                    docAmount != requiredPrice
                ) {
                    throw Exception("Payment status is '$docStatus'. Must be 'Verified' to activate.")
                }

                // Update the payment document: explicitly set isUsed = true, usedByDeviceId, usedByUserId, and usedAt
                val paymentUpdates = mapOf<String, Any>(
                    "isUsed" to true,
                    "status" to "Verified",
                    "usedByDeviceId" to androidId,
                    "usedByUserId" to userId,
                    "usedAt" to now,
                    "planType" to plan.lowercase()
                )
                transaction.update(paymentRef, paymentUpdates)

                // Set the user's subscription document to activate the pass
                val subData = mapOf(
                    "userId" to userId,
                    "planType" to plan.lowercase(),
                    "price" to requiredPrice,
                    "activeDeviceId" to androidId,
                    "activatedAt" to now,
                    "expiryMillis" to (now + durationMs),
                    "utr" to cleanUtr,
                    "status" to "active"
                )
                transaction.set(subscriptionRef, subData, SetOptions.merge())
            }.await()

            Log.i(TAG, "Successfully verified UTR $cleanUtr and activated plan '$plan' for user $userId on device $androidId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to verify UTR and activate pass: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Redeems user's reward points for a free pass using an atomic Firestore runTransaction.
     * Checks if user has enough points. If yes:
     * - Deducts requiredPoints from user's points balance.
     * - Updates expiryMillis based on plan (daily=24h, weekly=7d, monthly=30d).
     * - Sets activeDeviceId to androidId.
     * - Sets status to "active" and planType to plan.
     */
    suspend fun redeemPointsForPass(
        userId: String,
        androidId: String,
        plan: String,
        requiredPoints: Int,
        context: Context? = null
    ): Result<Unit> {
        return try {
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("Please sign in with Google to redeem points."))
            }

            val firebaseApp = try {
                if (context != null) {
                    FirebaseHelper.initialize(context)
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                try { FirebaseApp.getInstance() } catch (ex: Exception) { null }
            }

            val firestore = try {
                if (firebaseApp != null) FirebaseFirestore.getInstance(firebaseApp) else FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore not initialized: ${e.message}")
                return Result.failure(e)
            }

            val now = System.currentTimeMillis()
            val durationMs = when (plan.lowercase()) {
                "weekly" -> 7 * 24 * 3600 * 1000L
                "monthly" -> 30 * 24 * 3600 * 1000L
                else -> 24 * 3600 * 1000L // daily = 24h
            }

            val subscriptionRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(subscriptionRef)
                val currentPoints = if (snapshot.exists()) {
                    snapshot.getLong("points")?.toInt() ?: 0
                } else {
                    0
                }

                if (currentPoints < requiredPoints) {
                    throw IllegalStateException("Not enough points. Required: $requiredPoints 🪙, Available: $currentPoints 🪙. Share the app to earn more!")
                }

                val currentExpiry = if (snapshot.exists()) {
                    snapshot.getLong("expiryMillis") ?: 0L
                } else {
                    0L
                }

                val baseTime = if (currentExpiry > now) currentExpiry else now
                val newExpiry = baseTime + durationMs
                val newPoints = currentPoints - requiredPoints

                val updateData = mapOf(
                    "userId" to userId,
                    "points" to newPoints,
                    "planType" to plan.lowercase(),
                    "activeDeviceId" to androidId,
                    "expiryMillis" to newExpiry,
                    "status" to "active",
                    "activatedAt" to now,
                    "redeemedWithPoints" to true,
                    "redeemedPointsCost" to requiredPoints,
                    "lastRedemptionTime" to now
                )

                transaction.set(subscriptionRef, updateData, SetOptions.merge())
            }.await()

            Log.i(TAG, "Successfully redeemed $requiredPoints points for plan '$plan' for user $userId on device $androidId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to redeem points for pass: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generates or retrieves a unique 6-character referral code for a user based on their UID.
     */
    fun getReferralCodeForUser(userId: String): String {
        if (userId.isBlank()) return "RAPIDO"
        val hash = Math.abs(userId.hashCode()).toString(36).uppercase()
        return if (hash.length >= 6) hash.substring(0, 6) else hash.padEnd(6, 'X')
    }

    /**
     * Atomically applies a referral code when a user joins or submits a friend's referral link/code.
     * Transaction:
     * - Checks that the user is not applying their own referral code.
     * - Checks that the user hasn't already claimed a referral bonus ("referredBy" field).
     * - Finds the referrer user by searching user_subscriptions matching the referralCode or userId.
     * - Atomically grants 20 Points to the referrer (incrementing points by 20).
     * - Atomically grants 50 Points to the referred new user (incrementing points by 50).
     * - Sets "referredBy" and "referralAppliedAt" on the user's document to prevent re-application.
     * - Records the referral log in "user_referrals" collection for auditing.
     */
    suspend fun applyReferralCode(
        currentUserId: String,
        referralCodeInput: String,
        context: Context? = null
    ): Result<String> {
        return try {
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

            val firebaseApp = try {
                if (context != null) {
                    FirebaseHelper.initialize(context)
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                try { FirebaseApp.getInstance() } catch (ex: Exception) { null }
            }

            val firestore = try {
                if (firebaseApp != null) FirebaseFirestore.getInstance(firebaseApp) else FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore not initialized: ${e.message}")
                return Result.failure(e)
            }

            // Look up referrer user by referralCode or userId matching
            val queryByCode = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo("referralCode", cleanCode)
                .limit(1)
                .get()
                .await()

            var referrerDocId: String? = queryByCode.documents.firstOrNull()?.id

            // Fallback: If not indexed by field, match by computed referral code across recent users or direct UID match
            if (referrerDocId == null) {
                val directDoc = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(cleanCode).get().await()
                if (directDoc.exists()) {
                    referrerDocId = directDoc.id
                } else {
                    val allUsers = firestore.collection(COLLECTION_SUBSCRIPTIONS)
                        .limit(200)
                        .get()
                        .await()
                    for (doc in allUsers.documents) {
                        val uid = doc.id
                        if (getReferralCodeForUser(uid) == cleanCode || doc.getString("referralCode") == cleanCode) {
                            referrerDocId = uid
                            break
                        }
                    }
                }
            }

            if (referrerDocId == null || referrerDocId == currentUserId) {
                return Result.failure(IllegalArgumentException("Invalid or unrecognized referral code."))
            }

            val now = System.currentTimeMillis()
            val userRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(currentUserId)
            val referrerRef = firestore.collection(COLLECTION_SUBSCRIPTIONS).document(referrerDocId)
            val referralLogRef = firestore.collection(COLLECTION_REFERRALS).document("${referrerDocId}_$currentUserId")

            firestore.runTransaction { transaction ->
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
            }.await()

            Log.i(TAG, "Referral applied successfully: $currentUserId referred by $referrerDocId (+20 to referrer, +50 to new user)")
            Result.success("🎉 Referral Applied! You earned 50 Points and your friend earned 20 Points!")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to apply referral code: ${e.message}", e)
            Result.failure(e)
        }
    }
}

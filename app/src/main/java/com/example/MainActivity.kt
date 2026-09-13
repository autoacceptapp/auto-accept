package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.FirebaseApp
import java.text.SimpleDateFormat
import java.util.Date
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

// Slate & Emerald Theme Colors
private val Slate950 = Color(0xFF020617)
private val Slate900 = Color(0xFF0F172A)
private val Slate850 = Color(0xFF141E33)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val EmeraldGlow = Color(0x2E10B981)

private val Cyan500 = Color(0xFF06B6D4)
private val Cyan400 = Color(0xFF22D3EE)
private val Cyan300 = Color(0xFF67E8F9)
private val CyanGlow = Color(0x2E06B6D4)

private val Rose500 = Color(0xFFF43F5E)
private val Rose400 = Color(0xFFFB7185)
private val RoseGlow = Color(0x2EF43F5E)

private val Amber500 = Color(0xFFF59E0B)
private val Amber400 = Color(0xFFFBBF24)
private val AmberGlow = Color(0x2EF59E0B)

private val Purple400 = Color(0xFFC084FC)
private val PurpleGlow = Color(0x2EC084FC)

// OAuth 2.0 Web Client ID from Firebase google-services.json (client_type 3)
const val HARDCODED_WEB_CLIENT_ID = "675050958546-io1h9ka7evqgrrpccdk7qavniq2bu1vb.apps.googleusercontent.com"
const val KEY_PREMIUM = "isPremium"
const val KEY_SUBSCRIPTION_EXPIRY = "subscriptionExpiry"
const val KEY_LAST_UTR = "KEY_LAST_UTR"
const val KEY_PENDING_PLAN_NAME = "KEY_PENDING_PLAN_NAME"
const val KEY_PENDING_PLAN_DAYS = "KEY_PENDING_PLAN_DAYS"
const val KEY_PENDING_PLAN_PRICE = "KEY_PENDING_PLAN_PRICE"

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        val isPremiumState = mutableStateOf(false)
    }

    private var previewTts: TextToSpeech? = null
    private var isTtsReady = false

    private var pendingUtrListener: ListenerRegistration? = null

    // Runtime POST_NOTIFICATIONS permission launcher for Android 13+ (API 33)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted by user.")
        } else {
            Log.w("MainActivity", "POST_NOTIFICATIONS permission denied by user.")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt user for POST_NOTIFICATIONS permission on Android 13+ (API 33)
        checkNotificationPermission()

        FirebaseHelper.initialize(this)
        RemoteConfigManager.init(this)

        // Retrieve and log FCM registration token
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("MainActivity", "Firebase Cloud Messaging Token: $token")
                    val prefs = getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString("fcm_registration_token", token).apply()
                } else {
                    Log.w("MainActivity", "Fetching FCM registration token failed: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "FCM token retrieval error: ${e.message}")
        }

        // Load persisted ride history from local device storage
        AutoAcceptService.loadLocalLogs(this)

        // 5. Check permanent premium status & resume pending payment verification
        val isPrem = isUserPremium()
        isPremiumState.value = isPrem
        Log.d("MainActivity", "Permanent premium status on startup: $isPrem")
        resumePendingPaymentVerification()

        try {
            previewTts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.w("MainActivity", "Error starting preview TTS: ${e.message}")
        }

        setContent {
            MyApplicationTheme {
                AutoAcceptDashboardScreen(
                    onTestVoice = { testPhrase ->
                        speakTest(testPhrase)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ServiceStatusNotificationManager.updateStatus(this)
        ServiceStatusWidgetProvider.updateAllWidgets(this)
    }

    /**
     * 2. Fix Payment Overwrite Bug (Race Condition):
     * In saveUtrToFirebase(utr: String), execute a .get() request on the Firestore document BEFORE saving
     * the "PAYMENT_PENDING" state. If the document already exists and has status == "PAYMENT_VERIFIED" or "Verified",
     * directly call listenForPaymentStatus(utr) and DO NOT overwrite the document with "PENDING".
     */
    fun saveUtrToFirebase(
        utr: String,
        planName: String = "Daily",
        planDays: Int = 1,
        price: Int = 9
    ) {
        val cleanUtr = utr.trim()
        if (cleanUtr.isEmpty()) {
            Toast.makeText(this, "Please enter a valid 12-digit UTR.", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_UTR, cleanUtr)
            .putString(KEY_PENDING_PLAN_NAME, planName)
            .putInt(KEY_PENDING_PLAN_DAYS, planDays)
            .putInt(KEY_PENDING_PLAN_PRICE, price)
            .apply()

        val firestore = try {
            FirebaseHelper.initialize(this)
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("MainActivity", "Firestore not available: ${e.message}")
            null
        }

        if (firestore == null) {
            Toast.makeText(this, "Database unavailable. Please check your internet connection.", Toast.LENGTH_LONG).show()
            return
        }

        val docRef = firestore.collection("received_payments").document(cleanUtr)
        val user = FirebaseAuth.getInstance().currentUser
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        val paymentData = hashMapOf<String, Any>(
            "utr" to cleanUtr,
            "status" to "PAYMENT_PENDING",
            "planName" to planName,
            "planDays" to planDays,
            "price" to price,
            "userId" to (user?.uid ?: "anonymous"),
            "userEmail" to (user?.email ?: ""),
            "deviceId" to androidId,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(paymentData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("MainActivity", "Saved UTR $cleanUtr as PAYMENT_PENDING")
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to save UTR: ${e.message}", e)
            }
    }

    /**
     * Updates UI state based on permanent premium status stored locally.
     * The single source of truth for activating passes is SubscriptionManager.verifyUtrAndActivatePass.
     */
    fun updateUiForPremiumStatus() {
        val premium = isUserPremium()
        isPremiumState.value = premium
    }

    fun loadSavedSettings() {
        AutoAcceptService.loadLocalLogs(this)
    }

    fun isUserPremium(): Boolean {
        val prefs = getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean(KEY_PREMIUM, false)
        val expiry = prefs.getLong(KEY_SUBSCRIPTION_EXPIRY, 0L)
        return isPremium && (expiry > System.currentTimeMillis())
    }

    fun resumePendingPaymentVerification() {
        // Real-time listener bypass removed for security.
        // Subscription activation is strictly governed by the backend transaction in SubscriptionManager.
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            previewTts?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun speakTest(phrase: String) {
        AutoAcceptService.testVoiceAnnouncement(this)
        if (isTtsReady && previewTts != null) {
            val lang = AutoAcceptService.getTtsLanguage(this)
            val locale = AutoAcceptService.getLocaleForLanguage(lang)
            previewTts?.language = locale
            previewTts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "TestPreview_${System.currentTimeMillis()}")
            Toast.makeText(this, "Playing voice announcement (${locale.displayLanguage})...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingUtrListener?.remove()
        pendingUtrListener = null
        try {
            previewTts?.stop()
            previewTts?.shutdown()
            previewTts = null
        } catch (e: Exception) {
            Log.w("MainActivity", "Error shutting down preview TTS: ${e.message}")
        }
    }
}

/**
 * Main Single-Screen Dashboard:
 * - Background One-Tap Google Sign-In with robust fallback and error toasts
 * - Master Auto-Accept Switch (ON/OFF)
 * - Individual Pickup Distance Filter (ON/OFF)
 * - Individual Price / Fare Range Filter (ON/OFF)
 * - Individual Area / Keyword Blacklist Filter (ON/OFF)
 * - Voice Announcer (TTS) & Driver Name Configuration
 * - Accessibility Service Status & System Settings shortcut
 * - Live Console Activity Stream
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutoAcceptDashboardScreen(
    onTestVoice: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Runtime POST_NOTIFICATIONS permission launcher for Android 13+ (API 33)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Compose runtime POST_NOTIFICATIONS permission granted: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // -------------------------------------------------------------------------
    // FIREBASE AUTH & CREDENTIAL MANAGER STATE
    // -------------------------------------------------------------------------
    val firebaseAuth = remember {
        try {
            FirebaseHelper.initialize(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init error: ${e.message}", e)
            null
        }
    }

    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(firebaseAuth?.currentUser)
    }

    var isSigningIn by remember { mutableStateOf(false) }

    // Resolve Web Client ID: string resource with fallback to hardcoded Firebase Web Client ID
    val webClientId = remember {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val resStr = if (resId != 0) context.getString(resId) else ""
            if (resStr.isNotBlank() && !resStr.contains("YOUR_WEB_CLIENT_ID_HERE")) resStr else HARDCODED_WEB_CLIENT_ID
        } catch (e: Exception) {
            HARDCODED_WEB_CLIENT_ID
        }
    }

    // Keep currentUser in sync with FirebaseAuth
    DisposableEffect(firebaseAuth) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        firebaseAuth?.addAuthStateListener(listener)
        onDispose {
            firebaseAuth?.removeAuthStateListener(listener)
        }
    }

    // Google Sign-In helper function (Modern Credential Manager exclusively)
    fun triggerGoogleSignIn(isAutoSelect: Boolean = true) {
        if (isSigningIn) {
            Log.d("MainActivity", "Google Sign-In is already in progress. Ignoring duplicate request.")
            return
        }

        val activity = context as? Activity ?: run {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) break
                ctx = ctx.baseContext
            }
            ctx as? Activity
        }
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w("MainActivity", "Activity is not in a valid state for Google Sign-In.")
            return
        }

        val auth = try {
            firebaseAuth ?: if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null
        } catch (e: Exception) {
            Log.e("MainActivity", "Error retrieving FirebaseAuth: ${e.message}", e)
            null
        }
        if (auth == null) {
            val errorMsg = "Firebase Auth is not initialized or unavailable"
            Log.e("MainActivity", errorMsg)
            if (!isAutoSelect) {
                Toast.makeText(context, "Google Login Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
            return
        }

        val clientId = webClientId.trim()
        if (clientId.isBlank() || clientId.contains("YOUR_WEB_CLIENT_ID_HERE")) {
            val errorMsg = "Web Client ID is not configured"
            Log.w("MainActivity", errorMsg)
            if (!isAutoSelect) {
                Toast.makeText(context, "Google Login Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
            return
        }

        coroutineScope.launch {
            isSigningIn = true
            try {
                val credentialManager = CredentialManager.create(activity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(isAutoSelect)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    if (!idToken.isNullOrBlank()) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                        val signedInUser = authResult.user
                        currentUser = signedInUser

                        // Automatically initialize Driver Name if default
                        signedInUser?.displayName?.let { name ->
                            if (AutoAcceptService.getUserName(context) == AutoAcceptService.DEFAULT_USER_NAME) {
                                val firstName = name.split(" ").firstOrNull() ?: name
                                AutoAcceptService.setUserName(context, firstName)
                            }
                        }

                        (activity as? MainActivity)?.updateUiForPremiumStatus()
                        Toast.makeText(context, "Signed in as ${signedInUser?.displayName ?: signedInUser?.email ?: "User"}", Toast.LENGTH_SHORT).show()
                    } else {
                        throw IllegalStateException("Google ID token is empty")
                    }
                } else {
                    val errorMsg = "Unexpected credential format: ${credential.type}"
                    Log.w("MainActivity", errorMsg)
                    if (!isAutoSelect) {
                        Toast.makeText(context, "Google Login Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i("MainActivity", "User dismissed Google Sign-In prompt")
                if (!isAutoSelect) {
                    Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                // Expected and normal when no credentials are saved on device or during silent auto-select
                Log.d("MainActivity", "No credentials available for sign-in: ${e.message}")
                if (!isAutoSelect) {
                    Toast.makeText(context, "No Google account found on device. Please sign in or add an account in device Settings.", Toast.LENGTH_LONG).show()
                }
            } catch (e: GetCredentialException) {
                val rawMsg = e.localizedMessage ?: e.message ?: "Unknown credential error"
                if (isAutoSelect) {
                    Log.d("MainActivity", "Auto-select credential check: $rawMsg")
                } else {
                    Log.w("MainActivity", "Credential Manager warning: $rawMsg")
                    val userFriendlyMsg = when {
                        rawMsg.contains("16") || rawMsg.contains("Cannot find a matching credential") ->
                            "No matching Google account found, or SHA-1 debug fingerprint is missing in Firebase Console."
                        rawMsg.contains("10") || rawMsg.contains("DEVELOPER_ERROR") ->
                            "Developer error (10): Ensure SHA-1 debug certificate fingerprint is registered in Firebase Console."
                        else ->
                            "Google Sign-In note: $rawMsg"
                    }
                    Toast.makeText(context, userFriendlyMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.message ?: "Authentication failed"
                if (isAutoSelect) {
                    Log.d("MainActivity", "Silent sign-in check note: $errorMsg")
                } else {
                    Log.w("MainActivity", "Authentication warning: $errorMsg")
                    Toast.makeText(context, "Login failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } finally {
                isSigningIn = false
            }
        }
    }

    // Trigger One-Tap Google Sign-In on app start if user is not logged in
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            triggerGoogleSignIn(isAutoSelect = true)
        }
    }

    // -------------------------------------------------------------------------
    // IN-APP AUTO-UPDATE STATE (GITHUB RELEASES)
    // -------------------------------------------------------------------------
    var updateInfoToPrompt by remember { mutableStateOf<GitHubUpdateManager.UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            val result = GitHubUpdateManager.checkForUpdates(context)
            if (result is GitHubUpdateManager.UpdateCheckResult.UpdateAvailable) {
                updateInfoToPrompt = result.info
            }
        }
    }

    // Accessibility Service, Notification Listener & System Optimization statuses
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, AutoAcceptService::class.java))
    }
    var isNotificationListenerEnabled by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }
    var isOverlayAllowed by remember {
        mutableStateOf(canDrawOverlays(context))
    }

    var userDismissedPermissionsDialog by remember { mutableStateOf(false) }

    val hasMissingPermissions = !isAccessibilityEnabled || !isOverlayAllowed || !isBatteryOptimizationIgnored

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val acc = isAccessibilityServiceEnabled(context, AutoAcceptService::class.java)
                val notif = isNotificationListenerEnabled(context)
                val bat = isBatteryOptimizationIgnored(context)
                val ovl = canDrawOverlays(context)
                isAccessibilityEnabled = acc
                isNotificationListenerEnabled = notif
                isBatteryOptimizationIgnored = bat
                isOverlayAllowed = ovl
                if (acc && bat && ovl) {
                    userDismissedPermissionsDialog = false
                }
                ServiceStatusNotificationManager.updateStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Popup Permissions Dialog: automatically displays when any of the 3 critical permissions are missing
    if (hasMissingPermissions && !userDismissedPermissionsDialog) {
        MissingPermissionsDialog(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isOverlayAllowed = isOverlayAllowed,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            onOpenAccessibility = { openAccessibilitySettings(context) },
            onOpenOverlay = { openOverlaySettings(context) },
            onOpenBatteryOptimization = { openBatteryOptimizationSettings(context) },
            onDismiss = { userDismissedPermissionsDialog = true }
        )
    }

    // Wake Lock Screen State
    var isWakeLockOn by remember {
        mutableStateOf(AutoAcceptService.isWakeLockEnabled(context))
    }

    // -------------------------------------------------------------------------
    // DUAL-LOCK SUBSCRIPTION & FREE TRIAL STATE (SubscriptionManager)
    // -------------------------------------------------------------------------
    val androidId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    var subState by remember { mutableStateOf<SubState>(SubState.Loading) }
    var isActivatingPass by remember { mutableStateOf(false) }
    var isTransferringPass by remember { mutableStateOf(false) }
    var selectedPlanForPayment by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var isVerifyingPayment by remember { mutableStateOf(false) }

    // Plan Choice Dialog state: UPI vs Points
    data class PlanOption(
        val planKey: String,
        val title: String,
        val duration: String,
        val price: Int,
        val pointsCost: Int,
        val badge: String? = null
    )
    var selectedPlanForChoice by remember { mutableStateOf<PlanOption?>(null) }
    var isRedeemingPoints by remember { mutableStateOf(false) }

    val isPassActive = subState is SubState.TrialActive || subState is SubState.PremiumActive

    // Master Switch & Filters (Always operational in Freemium)
    var isMasterSwitchOn by remember {
        mutableStateOf(AutoAcceptService.isAutomationEnabled(context))
    }

    LaunchedEffect(androidId, currentUser?.uid) {
        SubscriptionManager.observeSubscription(context, androidId, currentUser?.uid)
            .collect { state ->
                subState = state
                val active = (state is SubState.TrialActive || state is SubState.PremiumActive)
                AutoAcceptService.setPassActive(context, active)
            }
    }

    var isDistanceFilterOn by remember { mutableStateOf(AutoAcceptService.isDistanceFilterEnabled(context)) }
    var maxDistanceInput by remember {
        val initialDist = AutoAcceptService.getMaxDistanceKm(context)
        mutableStateOf(if (initialDist % 1f == 0f) initialDist.toInt().toString() else initialDist.toString())
    }

    var isPriceFilterOn by remember { mutableStateOf(AutoAcceptService.isPriceFilterEnabled(context)) }
    var minPriceInput by remember { mutableStateOf(AutoAcceptService.getMinPrice(context).toInt().toString()) }
    var maxPriceInput by remember { mutableStateOf(AutoAcceptService.getMaxPrice(context).toInt().toString()) }

    var isBlacklistFilterOn by remember { mutableStateOf(AutoAcceptService.isBlacklistEnabled(context)) }
    var blacklistInput by remember { mutableStateOf(AutoAcceptService.getBlacklistKeywords(context)) }

    // Voice Announcer (TTS)
    var isTtsOn by remember { mutableStateOf(AutoAcceptService.isTtsEnabled(context)) }
    var ttsLanguage by remember { mutableStateOf(AutoAcceptService.getTtsLanguage(context)) }
    var userNameInput by remember { mutableStateOf(AutoAcceptService.getUserName(context)) }

    var isRapidoOnly by remember { mutableStateOf(AutoAcceptService.isTargetRapidoOnly(context)) }
    var acceptDelayMs by remember { mutableStateOf(AutoAcceptService.getAcceptDelayMs(context)) }

    // Live Logs & Event Stream
    val serviceLog by AutoAcceptService.recentLog.collectAsStateWithLifecycle()
    val serviceEvents by AutoAcceptService.serviceEvents.collectAsStateWithLifecycle()

    val isAcceptAllActive = isMasterSwitchOn && !isDistanceFilterOn && !isPriceFilterOn && !isBlacklistFilterOn

    // Active bottom navigation tab: 0 = Home (Controls), 1 = Profile (Earnings), 2 = History (Order Logs), 3 = Debug Logs, 4 = Settings
    val initialTargetTab = (context as? Activity)?.intent?.getIntExtra("TARGET_TAB", -1) ?: -1
    var selectedTab by rememberSaveable {
        mutableStateOf(if (initialTargetTab in 0..4) initialTargetTab else 0)
    }

    LaunchedEffect(Unit) {
        val target = (context as? Activity)?.intent?.getIntExtra("TARGET_TAB", -1) ?: -1
        if (target in 0..4) {
            selectedTab = target
        }
    }

    // -------------------------------------------------------------------------
    // LOCAL RIDE LOGS (Collected from AutoAcceptService)
    // -------------------------------------------------------------------------
    val rideLogs by AutoAcceptService.localRideLogs.collectAsStateWithLifecycle()
    val isLogsLoading = false

    val acceptedLogs = remember(rideLogs) {
        rideLogs.filter { it.status.equals("ACCEPTED", ignoreCase = true) }
    }
    val ignoredLogs = remember(rideLogs) {
        rideLogs.filter { it.status.equals("IGNORED", ignoreCase = true) }
    }
    val totalEarnings = remember(acceptedLogs) {
        acceptedLogs.sumOf { it.price.toDouble() }.toFloat()
    }
    val totalPickupKm = remember(acceptedLogs) {
        acceptedLogs.sumOf { it.pickupKm.toDouble() }.toFloat()
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isMasterSwitchOn && isAccessibilityEnabled) EmeraldGlow else Color(0x33EF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (isMasterSwitchOn && isAccessibilityEnabled) Emerald400 else Rose400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Rapido Auto Accept",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Slate50
                            )
                            Text(
                                text = "Driver Automation Suite",
                                fontSize = 10.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMasterSwitchOn && isAccessibilityEnabled) Color(0x2610B981) else Color(0x26EF4444),
                            border = BorderStroke(
                                1.dp,
                                if (isMasterSwitchOn && isAccessibilityEnabled) Color(0x4D10B981) else Color(0x4DEF4444)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isMasterSwitchOn && isAccessibilityEnabled) Emerald400 else Rose400)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMasterSwitchOn && isAccessibilityEnabled) "ONLINE" else "STANDBY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMasterSwitchOn && isAccessibilityEnabled) Emerald400 else Rose400
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (currentUser != null) {
                        val user = currentUser!!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.dp, Slate800, CircleShape)
                                .clickable {
                                    coroutineScope.launch {
                                        try {
                                            firebaseAuth?.signOut()
                                            val credMgr = CredentialManager.create(context)
                                            credMgr.clearCredentialState(ClearCredentialStateRequest())
                                            currentUser = null
                                            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.w("MainActivity", "Sign out note: ${e.message}")
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Cyan500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user.displayName?.take(1) ?: user.email?.take(1) ?: "U").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = user.displayName?.split(" ")?.firstOrNull() ?: "User",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.dp, Slate800, CircleShape)
                                .clickable {
                                    triggerGoogleSignIn(isAutoSelect = false)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Sign In",
                                tint = Cyan400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSigningIn) "Signing in..." else "Sign In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Slate50
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate950,
                contentColor = Slate100,
                tonalElevation = 8.dp,
                modifier = Modifier.border(BorderStroke(1.dp, Slate900))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Home Controls"
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald400,
                        selectedTextColor = Emerald400,
                        indicatorColor = Slate800,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Earnings"
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald400,
                        selectedTextColor = Emerald400,
                        indicatorColor = Slate800,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Order History"
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald400,
                        selectedTextColor = Emerald400,
                        indicatorColor = Slate800,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug Logs"
                        )
                    },
                    label = {
                        Text(
                            text = "Logs",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Cyan400,
                        selectedTextColor = Cyan400,
                        indicatorColor = Slate800,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_logs")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald400,
                        selectedTextColor = Emerald400,
                        indicatorColor = Slate800,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

            // =========================================================================
            // 0. DUAL-LOCK SUBSCRIPTION & PAYWALL SYSTEM CARD
            // =========================================================================
            when (val s = subState) {
                is SubState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_loading_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Cyan400
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Verifying pass & free trial...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate300
                            )
                        }
                    }
                }

                is SubState.TrialActive, is SubState.PremiumActive -> {
                    val isPremium = s is SubState.PremiumActive
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_active_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(
                            1.dp,
                            if (isPremium) Cyan400.copy(alpha = 0.6f) else Emerald500.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isPremium) CyanGlow else EmeraldGlow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Pass Active",
                                            tint = if (isPremium) Cyan400 else Emerald400,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "Pass Active",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate50
                                        )
                                        Text(
                                            text = if (isPremium) {
                                                "${(s as SubState.PremiumActive).planType.replaceFirstChar { it.uppercase() }} Pass Active"
                                            } else {
                                                "Free Trial (${(s as SubState.TrialActive).hoursLeft}h left)"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isPremium) Cyan400 else Emerald400
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isPremium) CyanGlow else EmeraldGlow,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPremium) Cyan500.copy(alpha = 0.6f) else Emerald500.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Text(
                                        text = if (isPremium) {
                                            (s as SubState.PremiumActive).planType.uppercase() + " PASS"
                                        } else {
                                            "TRIAL ACTIVE"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        color = if (isPremium) Cyan400 else Emerald400,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Pass details & Expiration timestamp
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Slate950,
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Expires:",
                                                fontSize = 12.sp,
                                                color = Slate400
                                            )
                                        }
                                        val expiryMillis = if (isPremium) {
                                            (s as SubState.PremiumActive).expiryMillis
                                        } else {
                                            (s as SubState.TrialActive).expiryMillis
                                        }
                                        val expiryStr = remember(expiryMillis) {
                                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(expiryMillis))
                                        }
                                        Text(
                                            text = expiryStr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate200
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DEVICE ID",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate500,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = androidId,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate400,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is SubState.DeviceMismatch -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_mismatch_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33F43F5E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Device Mismatch",
                                        tint = Rose400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Device Mismatch",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate50
                                    )
                                    Text(
                                        text = "Your pass is active on another device.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Rose400
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x1AF43F5E),
                                border = BorderStroke(1.dp, Color(0x4DF43F5E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Your subscription is currently locked to:",
                                        fontSize = 12.sp,
                                        color = Slate300
                                    )
                                    Text(
                                        text = s.activeDeviceId,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Rose400
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Only 1 device can use this subscription pass at a time. Click below to transfer the pass to this device.",
                                        fontSize = 12.sp,
                                        color = Slate400,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val uid = currentUser?.uid
                                    if (uid.isNullOrBlank()) {
                                        Toast.makeText(context, "Please sign in with Google to transfer your pass", Toast.LENGTH_SHORT).show()
                                        triggerGoogleSignIn(isAutoSelect = false)
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isTransferringPass = true
                                        val res = SubscriptionManager.transferSubscription(uid, androidId)
                                        isTransferringPass = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Pass transferred to this device successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Transfer failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isTransferringPass,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Rose500,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("transfer_pass_button")
                            ) {
                                if (isTransferringPass) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Transferring...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Transfer Pass to this Device", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is SubState.TrialExpired, is SubState.NoSubscription -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paywall_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(AmberGlow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Paywall",
                                            tint = Amber400,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (s is SubState.TrialExpired) "Free Trial Expired" else "Subscription Required",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate50
                                        )
                                        Text(
                                            text = "Choose a pass to auto-accept rides",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Amber400
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = AmberGlow,
                                    border = BorderStroke(1.dp, Amber500.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "PAYWALL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        color = Amber400,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Your 48-hour free trial has expired. Select a pass to continue receiving and auto-accepting Rapido orders instantly.",
                                fontSize = 12.sp,
                                color = Slate300,
                                lineHeight = 17.sp
                            )

                            // 3 Plan Cards (Daily, Weekly, Monthly)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Plan 1: Daily (₹9)
                                PaywallPlanItem(
                                    title = "Daily",
                                    duration = "24 Hours",
                                    price = "₹9",
                                    pointsText = "or ${SubscriptionManager.POINTS_DAILY_PASS} 🪙",
                                    isPopular = false,
                                    badge = null,
                                    isActivating = isVerifyingPayment || isRedeemingPoints,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val uid = currentUser?.uid
                                        if (uid.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google to purchase a pass", Toast.LENGTH_SHORT).show()
                                            triggerGoogleSignIn(isAutoSelect = false)
                                            return@PaywallPlanItem
                                        }
                                        selectedPlanForChoice = PlanOption("daily", "Daily", "24 Hours", 9, SubscriptionManager.POINTS_DAILY_PASS, null)
                                    }
                                )

                                // Plan 2: Weekly (₹49)
                                PaywallPlanItem(
                                    title = "Weekly",
                                    duration = "7 Days",
                                    price = "₹49",
                                    pointsText = "or ${SubscriptionManager.POINTS_WEEKLY_PASS} 🪙",
                                    isPopular = true,
                                    badge = "POPULAR",
                                    isActivating = isVerifyingPayment || isRedeemingPoints,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val uid = currentUser?.uid
                                        if (uid.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google to purchase a pass", Toast.LENGTH_SHORT).show()
                                            triggerGoogleSignIn(isAutoSelect = false)
                                            return@PaywallPlanItem
                                        }
                                        selectedPlanForChoice = PlanOption("weekly", "Weekly", "7 Days", 49, SubscriptionManager.POINTS_WEEKLY_PASS, "POPULAR")
                                    }
                                )

                                // Plan 3: Monthly (₹179)
                                PaywallPlanItem(
                                    title = "Monthly",
                                    duration = "30 Days",
                                    price = "₹179",
                                    pointsText = "or ${SubscriptionManager.POINTS_MONTHLY_PASS} 🪙",
                                    isPopular = false,
                                    badge = "SAVE 35%",
                                    isActivating = isVerifyingPayment || isRedeemingPoints,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val uid = currentUser?.uid
                                        if (uid.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google to purchase a pass", Toast.LENGTH_SHORT).show()
                                            triggerGoogleSignIn(isAutoSelect = false)
                                            return@PaywallPlanItem
                                        }
                                        selectedPlanForChoice = PlanOption("monthly", "Monthly", "30 Days", 179, SubscriptionManager.POINTS_MONTHLY_PASS, "SAVE 35%")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Choose Payment Method (UPI vs Redeem Points) Dialog
            if (selectedPlanForChoice != null) {
                val plan = selectedPlanForChoice!!
                val currentPoints = subState.points
                AlertDialog(
                    onDismissRequest = {
                        if (!isRedeemingPoints) selectedPlanForChoice = null
                    },
                    shape = RoundedCornerShape(22.dp),
                    containerColor = Slate900,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Activate ${plan.title} Pass",
                                fontWeight = FontWeight.Bold,
                                color = Slate50,
                                fontSize = 18.sp
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Select how you'd like to activate your ${plan.duration} pass:",
                                fontSize = 13.sp,
                                color = Slate300
                            )

                            // Current Points indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate950,
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Points Balance:",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$currentPoints",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Amber400
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "🪙", fontSize = 13.sp)
                                    }
                                }
                            }

                            // Choice 1: Pay via UPI
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Slate850,
                                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isRedeemingPoints) {
                                        val p = selectedPlanForChoice
                                        selectedPlanForChoice = null
                                        if (p != null) {
                                            selectedPlanForPayment = Pair(p.planKey, p.price)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldGlow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payment,
                                                contentDescription = null,
                                                tint = Emerald400,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Pay via UPI",
                                                fontWeight = FontWeight.Bold,
                                                color = Slate50,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Instant QR / UTR verification",
                                                fontSize = 11.sp,
                                                color = Slate400
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₹${plan.price}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Emerald400
                                    )
                                }
                            }

                            // Choice 2: Redeem Points
                            val hasEnoughPoints = currentPoints >= plan.pointsCost
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (hasEnoughPoints) Slate850 else Slate950,
                                border = BorderStroke(
                                    1.dp,
                                    if (hasEnoughPoints) Amber400.copy(alpha = 0.5f) else Slate800
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isRedeemingPoints) {
                                        if (!hasEnoughPoints) {
                                            Toast.makeText(
                                                context,
                                                "Not enough points. Share the app to earn more!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@clickable
                                        }
                                        val uid = currentUser?.uid
                                        if (uid.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                                            triggerGoogleSignIn(isAutoSelect = false)
                                            return@clickable
                                        }

                                        coroutineScope.launch {
                                            isRedeemingPoints = true
                                            val res = SubscriptionManager.redeemPointsForPass(
                                                userId = uid,
                                                androidId = androidId,
                                                plan = plan.planKey,
                                                requiredPoints = plan.pointsCost,
                                                context = context
                                            )
                                            isRedeemingPoints = false

                                            if (res.isSuccess) {
                                                val prefs = context.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
                                                val durationMs = when (plan.planKey.lowercase()) {
                                                    "weekly" -> 7 * 24 * 3600 * 1000L
                                                    "monthly" -> 30 * 24 * 3600 * 1000L
                                                    else -> 24 * 3600 * 1000L
                                                }
                                                val expiry = System.currentTimeMillis() + durationMs
                                                prefs.edit()
                                                    .putBoolean(KEY_PREMIUM, true)
                                                    .putLong(KEY_SUBSCRIPTION_EXPIRY, expiry)
                                                    .putString("activePlan", plan.title)
                                                    .apply()

                                                (context as? MainActivity)?.updateUiForPremiumStatus()
                                                (context as? MainActivity)?.loadSavedSettings()

                                                Toast.makeText(
                                                    context,
                                                    "🎉 ${plan.title} Pass activated! Deducted ${plan.pointsCost} 🪙",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                selectedPlanForChoice = null
                                            } else {
                                                val err = res.exceptionOrNull()?.message ?: "Redemption failed"
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(AmberGlow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🪙", fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Redeem Points",
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasEnoughPoints) Slate50 else Slate400,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (hasEnoughPoints) "Free pass with points" else "Need ${plan.pointsCost - currentPoints} more points",
                                                fontSize = 11.sp,
                                                color = if (hasEnoughPoints) Amber400 else Slate500
                                            )
                                        }
                                    }

                                    if (isRedeemingPoints) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Amber400,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "${plan.pointsCost} 🪙",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (hasEnoughPoints) Amber400 else Slate500
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        OutlinedButton(
                            onClick = { selectedPlanForChoice = null },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate700),
                            enabled = !isRedeemingPoints
                        ) {
                            Text("Cancel", color = Slate300)
                        }
                    }
                )
            }

            // UPI Payment & UTR Verification Dialog on Dashboard
            if (selectedPlanForPayment != null) {
                val (planKey, price) = selectedPlanForPayment!!
                PaymentDialog(
                    planName = planKey,
                    price = price,
                    isVerifying = isVerifyingPayment,
                    onDismiss = {
                        if (!isVerifyingPayment) selectedPlanForPayment = null
                    },
                    onVerify = { utr ->
                        val uid = currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
                            triggerGoogleSignIn(isAutoSelect = false)
                            return@PaymentDialog
                        }
                        val planDays = when (planKey.lowercase()) {
                            "weekly" -> 7
                            "monthly" -> 30
                            else -> 1
                        }
                        (context as? MainActivity)?.saveUtrToFirebase(
                            utr = utr,
                            planName = planKey.replaceFirstChar { it.uppercase() },
                            planDays = planDays,
                            price = price
                        )
                        coroutineScope.launch {
                            isVerifyingPayment = true
                            val res = SubscriptionManager.verifyUtrAndActivatePass(
                                userId = uid,
                                plan = planKey,
                                androidId = androidId,
                                utr = utr,
                                context = context
                            )
                            isVerifyingPayment = false
                            if (res.isSuccess) {
                                val prefs = (context as? MainActivity)?.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
                                val durationMs = when (planKey.lowercase()) {
                                    "weekly" -> 7 * 24 * 3600 * 1000L
                                    "monthly" -> 30 * 24 * 3600 * 1000L
                                    else -> 24 * 3600 * 1000L
                                }
                                val expiry = System.currentTimeMillis() + durationMs
                                prefs?.edit()
                                    ?.putBoolean(KEY_PREMIUM, true)
                                    ?.putLong(KEY_SUBSCRIPTION_EXPIRY, expiry)
                                    ?.putString("activePlan", planKey.replaceFirstChar { it.uppercase() })
                                    ?.remove(KEY_PENDING_PLAN_NAME)
                                    ?.remove(KEY_PENDING_PLAN_DAYS)
                                    ?.remove(KEY_PENDING_PLAN_PRICE)
                                    ?.remove(KEY_LAST_UTR)
                                    ?.apply()

                                (context as? MainActivity)?.updateUiForPremiumStatus()
                                (context as? MainActivity)?.loadSavedSettings()

                                Toast.makeText(
                                    context,
                                    "Payment verified! ${planKey.replaceFirstChar { it.uppercase() }} Pass activated successfully.",
                                    Toast.LENGTH_LONG
                                ).show()
                                selectedPlanForPayment = null
                            } else {
                                val errorMsg = res.exceptionOrNull()?.message ?: "Verification failed. Please check your UTR."
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }

            // =========================================================================
            // SERVICE STATUS MONITOR DASHBOARD WIDGET
            // =========================================================================
            ServiceStatusDashboardWidget(
                onNavigateToDebugLogs = { selectedTab = 3 }
            )

            // =========================================================================
            // 1. MASTER AUTO-ACCEPT SWITCH CARD
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_switch_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(
                    1.dp,
                    when {
                        isMasterSwitchOn && isPassActive -> Emerald500.copy(alpha = 0.6f)
                        isMasterSwitchOn && !isPassActive -> Cyan500.copy(alpha = 0.6f)
                        else -> Slate800
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isMasterSwitchOn && isPassActive -> EmeraldGlow
                                            isMasterSwitchOn && !isPassActive -> CyanGlow
                                            else -> Color(0x33EF4444)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Master Power",
                                    tint = when {
                                        isMasterSwitchOn && isPassActive -> Emerald400
                                        isMasterSwitchOn && !isPassActive -> Cyan400
                                        else -> Rose400
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Master Auto-Accept",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50
                                )
                                Text(
                                    text = when {
                                        !isMasterSwitchOn -> "ENGINE HALTED (OFF)"
                                        isPassActive -> "PREMIUM ENGINE ACTIVE (100-600ms Speed)"
                                        else -> "FREE MODE ACTIVE (1-2s Delay, Accept-All)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when {
                                        !isMasterSwitchOn -> Rose400
                                        isPassActive -> Emerald400
                                        else -> Cyan400
                                    }
                                )
                            }
                        }

                        Switch(
                            checked = isMasterSwitchOn,
                            onCheckedChange = { checked ->
                                isMasterSwitchOn = checked
                                AutoAcceptService.setAutomationEnabled(context, checked)
                            },
                            enabled = true,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isPassActive) Emerald500 else Cyan500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            ),
                            modifier = Modifier.testTag("master_switch_toggle")
                        )
                    }

                    // Mode Status Banner
                    if (isMasterSwitchOn) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPassActive) Color(0x2610B981) else Color(0x2606B6D4),
                            border = BorderStroke(1.dp, if (isPassActive) Color(0x4D10B981) else Color(0x4D06B6D4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPassActive) Icons.Default.FlashOn else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isPassActive) Emerald400 else Cyan400,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPassActive) {
                                        if (isAcceptAllActive) {
                                            "Premium Accept-All: Instant high speed (100-600ms) with no filter restrictions."
                                        } else {
                                            "Premium Engine Active: Lightning speed (100-600ms) with active pickup, fare & blacklist filters."
                                        }
                                    } else {
                                        "Free Mode Active: Accepts all orders with 1-2s delay. Upgrade to Premium for 100-600ms instant speed & custom filters."
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPassActive) Slate100 else Slate200
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. DISTANCE FILTER CARD (PREMIUM FEATURE)
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isPassActive) 1f else 0.5f)
                    .testTag("distance_filter_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDistanceFilterOn && isPassActive) EmeraldGlow else Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Distance Filter Icon",
                                    tint = if (isDistanceFilterOn && isPassActive) Emerald400 else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Max Pickup Distance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50
                                )
                                Text(
                                    text = if (!isPassActive) "PREMIUM ONLY" else if (isDistanceFilterOn) "Active: Accept <= ${maxDistanceInput.ifBlank { "0" }} km" else "Filter Disabled",
                                    fontSize = 11.sp,
                                    color = if (!isPassActive) Amber400 else if (isDistanceFilterOn) Emerald400 else Slate400
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isPassActive) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33F59E0B),
                                    border = BorderStroke(1.dp, Color(0x66F59E0B))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Amber400,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PREMIUM",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Amber400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Switch(
                                checked = isDistanceFilterOn && isPassActive,
                                onCheckedChange = { checked ->
                                    if (isPassActive) {
                                        isDistanceFilterOn = checked
                                        AutoAcceptService.setDistanceFilterEnabled(context, checked)
                                    }
                                },
                                enabled = isPassActive,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Emerald500,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate800,
                                    disabledUncheckedThumbColor = Slate600,
                                    disabledUncheckedTrackColor = Slate850
                                ),
                                modifier = Modifier.testTag("distance_filter_toggle")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = maxDistanceInput,
                        onValueChange = { newValue ->
                            if (isPassActive) {
                                val filtered = newValue.filter { it.isDigit() || it == '.' }
                                maxDistanceInput = filtered
                                filtered.toFloatOrNull()?.let { dist ->
                                    AutoAcceptService.setMaxDistanceKm(context, dist)
                                }
                            }
                        },
                        enabled = isPassActive && isDistanceFilterOn,
                        label = { Text("Max Distance in Kilometers", color = Slate400) },
                        placeholder = { Text("e.g. 2.5", color = Slate500) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("max_distance_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("1.5", "2.0", "3.0", "5.0").forEach { preset ->
                            FilterChip(
                                selected = maxDistanceInput == preset && isDistanceFilterOn && isPassActive,
                                onClick = {
                                    if (isPassActive && isDistanceFilterOn) {
                                        maxDistanceInput = preset
                                        AutoAcceptService.setMaxDistanceKm(context, preset.toFloat())
                                    }
                                },
                                enabled = isPassActive && isDistanceFilterOn,
                                label = { Text("$preset km", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. PRICE / FARE RANGE FILTER CARD (PREMIUM FEATURE)
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isPassActive) 1f else 0.5f)
                    .testTag("price_filter_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isPriceFilterOn && isPassActive) EmeraldGlow else Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyRupee,
                                    contentDescription = "Price Filter Icon",
                                    tint = if (isPriceFilterOn && isPassActive) Emerald400 else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Fare Range Filter",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50
                                )
                                Text(
                                    text = if (!isPassActive) "PREMIUM ONLY" else if (isPriceFilterOn) "Accept ₹${minPriceInput.ifBlank { "0" }} - ₹${maxPriceInput.ifBlank { "∞" }}" else "Filter Disabled",
                                    fontSize = 11.sp,
                                    color = if (!isPassActive) Amber400 else if (isPriceFilterOn) Emerald400 else Slate400
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isPassActive) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33F59E0B),
                                    border = BorderStroke(1.dp, Color(0x66F59E0B))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Amber400,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PREMIUM",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Amber400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Switch(
                                checked = isPriceFilterOn && isPassActive,
                                onCheckedChange = { checked ->
                                    if (isPassActive) {
                                        isPriceFilterOn = checked
                                        AutoAcceptService.setPriceFilterEnabled(context, checked)
                                    }
                                },
                                enabled = isPassActive,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Emerald500,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate800,
                                    disabledUncheckedThumbColor = Slate600,
                                    disabledUncheckedTrackColor = Slate850
                                ),
                                modifier = Modifier.testTag("price_filter_toggle")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = minPriceInput,
                            onValueChange = { newValue ->
                                if (isPassActive) {
                                    val filtered = newValue.filter { it.isDigit() }
                                    minPriceInput = filtered
                                    filtered.toFloatOrNull()?.let { p ->
                                        AutoAcceptService.setMinPrice(context, p)
                                    }
                                }
                            },
                            enabled = isPassActive && isPriceFilterOn,
                            label = { Text("Min Fare (₹)", color = Slate400) },
                            placeholder = { Text("0", color = Slate500) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("min_price_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = maxPriceInput,
                            onValueChange = { newValue ->
                                if (isPassActive) {
                                    val filtered = newValue.filter { it.isDigit() }
                                    maxPriceInput = filtered
                                    filtered.toFloatOrNull()?.let { p ->
                                        AutoAcceptService.setMaxPrice(context, p)
                                    }
                                }
                            },
                            enabled = isPassActive && isPriceFilterOn,
                            label = { Text("Max Fare (₹)", color = Slate400) },
                            placeholder = { Text("0 = No cap", color = Slate500) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("max_price_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("₹40+" to 40f, "₹80+" to 80f, "₹120+" to 120f, "₹200+" to 200f).forEach { (label, minVal) ->
                            FilterChip(
                                selected = minPriceInput == minVal.toInt().toString() && isPriceFilterOn && isPassActive,
                                onClick = {
                                    if (isPassActive && isPriceFilterOn) {
                                        minPriceInput = minVal.toInt().toString()
                                        AutoAcceptService.setMinPrice(context, minVal)
                                    }
                                },
                                enabled = isPassActive && isPriceFilterOn,
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. BLACKLIST KEYWORDS FILTER CARD (PREMIUM FEATURE)
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isPassActive) 1f else 0.5f)
                    .testTag("blacklist_filter_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isBlacklistFilterOn && isPassActive) Color(0x33EF4444) else Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Blacklist Filter Icon",
                                    tint = if (isBlacklistFilterOn && isPassActive) Rose400 else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Area / Keyword Blacklist",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50
                                )
                                Text(
                                    text = if (!isPassActive) "PREMIUM ONLY" else if (isBlacklistFilterOn) "Rejects orders matching words" else "Filter Disabled",
                                    fontSize = 11.sp,
                                    color = if (!isPassActive) Amber400 else if (isBlacklistFilterOn) Rose400 else Slate400
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isPassActive) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33F59E0B),
                                    border = BorderStroke(1.dp, Color(0x66F59E0B))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Amber400,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PREMIUM",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Amber400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Switch(
                                checked = isBlacklistFilterOn && isPassActive,
                                onCheckedChange = { checked ->
                                    if (isPassActive) {
                                        isBlacklistFilterOn = checked
                                        AutoAcceptService.setBlacklistEnabled(context, checked)
                                    }
                                },
                                enabled = isPassActive,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Rose500,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate800,
                                    disabledUncheckedThumbColor = Slate600,
                                    disabledUncheckedTrackColor = Slate850
                                ),
                                modifier = Modifier.testTag("blacklist_filter_toggle")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = blacklistInput,
                        onValueChange = { newText ->
                            if (isPassActive) {
                                blacklistInput = newText
                                AutoAcceptService.setBlacklistKeywords(context, newText)
                            }
                        },
                        enabled = isPassActive && isBlacklistFilterOn,
                        label = { Text("Rejected Areas / Keywords (comma-separated)", color = Slate400) },
                        placeholder = { Text("e.g. Tollgate, Slum, Highway, Ghatkopar", color = Slate500) },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blacklist_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // =========================================================================
            // 5. REAL-TIME VISUAL LOG VIEW (Events & Telemetry)
            // =========================================================================
            VisualLogViewCard(
                serviceEvents = serviceEvents,
                rawServiceLog = serviceLog,
                isMasterSwitchOn = isMasterSwitchOn,
                isServiceRunning = isAccessibilityEnabled,
                onSimulateTest = { AutoAcceptService.simulateTestEvent() },
                onClearEvents = { AutoAcceptService.clearServiceEvents() }
            )
        }
    }
    1 -> {
        ProfileEarningsTabContent(
            currentUser = currentUser,
            androidId = androidId,
            rideLogs = rideLogs,
            acceptedLogs = acceptedLogs,
            totalEarnings = totalEarnings,
            totalPickupKm = totalPickupKm,
            onSignOut = {
                coroutineScope.launch {
                    try {
                        firebaseAuth?.signOut()
                        val credMgr = CredentialManager.create(context)
                        credMgr.clearCredentialState(ClearCredentialStateRequest())
                        currentUser = null
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Sign out note: ${e.message}")
                    }
                }
            },
            onSignIn = {
                triggerGoogleSignIn(isAutoSelect = false)
            },
            subState = subState
        )
    }
    2 -> {
        OrderHistoryTabContent(
            rideLogs = rideLogs,
            acceptedLogs = acceptedLogs,
            ignoredLogs = ignoredLogs,
            totalEarnings = totalEarnings,
            totalPickupKm = totalPickupKm,
            isLogsLoading = isLogsLoading
        )
    }
    3 -> {
        DebugLogsScreen(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isNotificationListenerEnabled = isNotificationListenerEnabled,
            onOpenAccessibility = { openAccessibilitySettings(context) },
            onOpenNotificationListener = { openNotificationListenerSettings(context) }
        )
    }
    4 -> {
        SettingsTabContent(
            isPassActive = isPassActive,
            isWakeLockOn = isWakeLockOn,
            onWakeLockChange = { checked ->
                if (isPassActive) {
                    isWakeLockOn = checked
                    AutoAcceptService.setWakeLockEnabled(context, checked)
                }
            },
            isTtsOn = isTtsOn,
            onTtsChange = { checked ->
                if (isPassActive) {
                    isTtsOn = checked
                    AutoAcceptService.setTtsEnabled(context, checked)
                }
            },
            userNameInput = userNameInput,
            onUserNameChange = { newName ->
                if (isPassActive) {
                    userNameInput = newName
                    AutoAcceptService.setUserName(context, newName)
                }
            },
            ttsLanguage = ttsLanguage,
            onTtsLanguageChange = { lang ->
                if (isPassActive && isTtsOn) {
                    ttsLanguage = lang
                    AutoAcceptService.setTtsLanguage(context, lang)
                }
            },
            onTestVoice = { sampleSpeech ->
                onTestVoice(sampleSpeech)
            },
            isRapidoOnly = isRapidoOnly,
            onRapidoOnlyChange = { checked ->
                isRapidoOnly = checked
                AutoAcceptService.setTargetRapidoOnly(context, checked)
            },
            isMasterSwitchOn = isMasterSwitchOn,
            onMasterSwitchChange = { checked ->
                isMasterSwitchOn = checked
                AutoAcceptService.setAutomationEnabled(context, checked)
            },
            acceptDelayMs = acceptDelayMs,
            onAcceptDelayChange = { delay ->
                acceptDelayMs = delay
                AutoAcceptService.setAcceptDelayMs(context, delay)
            },
            isAccessibilityEnabled = isAccessibilityEnabled,
            isNotificationListenerEnabled = isNotificationListenerEnabled,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            isOverlayAllowed = isOverlayAllowed,
            onOpenAccessibility = { openAccessibilitySettings(context) },
            onOpenNotificationListener = { openNotificationListenerSettings(context) },
            onOpenBatteryOptimization = { openBatteryOptimizationSettings(context) },
            onOpenOverlay = { openOverlaySettings(context) },
            onRefreshPermissions = {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context, AutoAcceptService::class.java)
                isNotificationListenerEnabled = isNotificationListenerEnabled(context)
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
                isOverlayAllowed = canDrawOverlays(context)
                ServiceStatusNotificationManager.updateStatus(context)
            },
            onUpdateAvailable = { updateInfo ->
                updateInfoToPrompt = updateInfo
            }
        )
    }
}
}

        // =========================================================================
        // IN-APP AUTO-UPDATE PROMPT DIALOG
        // =========================================================================
        if (updateInfoToPrompt != null) {
            val info = updateInfoToPrompt!!
            AlertDialog(
                onDismissRequest = {
                    if (!isDownloadingUpdate) {
                        updateInfoToPrompt = null
                    }
                },
                containerColor = Slate900,
                iconContentColor = Cyan400,
                titleContentColor = Slate50,
                textContentColor = Slate300,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "System Update",
                            tint = Cyan400,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "New Update Available",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate50
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A new release has been detected on the GitHub repository.",
                            fontSize = 13.sp,
                            color = Slate300
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current: v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGlow,
                                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Latest: ${info.tagName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (info.releaseTitle.isNotBlank() && info.releaseTitle != info.tagName) {
                            Text(
                                text = info.releaseTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Slate100
                            )
                        }

                        if (isDownloadingUpdate) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Emerald400,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Downloading update in background...",
                                    fontSize = 12.sp,
                                    color = Emerald400
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDownloadingUpdate = true
                            GitHubUpdateManager.startDownloadAndInstall(context, info) {
                                isDownloadingUpdate = false
                                updateInfoToPrompt = null
                            }
                        },
                        enabled = !isDownloadingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald500,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (isDownloadingUpdate) "Downloading..." else "Download & Install", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    if (!isDownloadingUpdate) {
                        OutlinedButton(
                            onClick = { updateInfoToPrompt = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Text("Later")
                        }
                    }
                }
            )
        }
    }
}



@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(EmeraldGlow)
                    .border(2.dp, Emerald400.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "App Logo",
                    tint = Emerald400,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Rapido Auto Accept",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Slate50,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Premium Automation Suite",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = Cyan400,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = "Loading license data...",
                fontSize = 12.sp,
                color = Slate400,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Visual Log View Card in Dashboard:
 * Displays recent events caught by AutoAcceptService such as 'Ride detected',
 * 'Order accepted', 'Order queued', or filter rejections.
 */
@Composable
fun VisualLogViewCard(
    serviceEvents: List<ServiceEvent>,
    rawServiceLog: String,
    isMasterSwitchOn: Boolean,
    isServiceRunning: Boolean,
    onSimulateTest: () -> Unit,
    onClearEvents: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0 = All, 1 = Detected, 2 = Accepted, 3 = Filtered
    var isExpanded by remember { mutableStateOf(false) }
    var showRawConsole by remember { mutableStateOf(false) }

    val filteredEvents = remember(serviceEvents, selectedFilter) {
        when (selectedFilter) {
            1 -> serviceEvents.filter { it.type == ServiceEventType.RIDE_DETECTED }
            2 -> serviceEvents.filter { it.type == ServiceEventType.ORDER_ACCEPTED }
            3 -> serviceEvents.filter { it.type == ServiceEventType.ORDER_IGNORED }
            else -> serviceEvents
        }
    }

    val detectedCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.RIDE_DETECTED }
    }
    val acceptedCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.ORDER_ACCEPTED }
    }
    val ignoredCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.ORDER_IGNORED }
    }

    val displayedEvents = if (isExpanded) filteredEvents else filteredEvents.take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visual_log_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar Live Feed",
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Live Activity Feed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            // Live status indicator pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isMasterSwitchOn && isServiceRunning) EmeraldGlow else Color(0x2EEF4444),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMasterSwitchOn && isServiceRunning) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isMasterSwitchOn && isServiceRunning) Emerald400 else Rose400)
                                    )
                                    Text(
                                        text = if (isMasterSwitchOn && isServiceRunning) "LIVE" else "PAUSED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMasterSwitchOn && isServiceRunning) Emerald400 else Rose400
                                    )
                                }
                            }
                        }
                        Text(
                            text = "AutoAccept real-time event telemetry",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                // Action icons: Simulate Test / Clear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = onSimulateTest,
                        shape = RoundedCornerShape(10.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier.testTag("simulate_event_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Simulate",
                                tint = Cyan300,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Simulate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan300
                            )
                        }
                    }

                    if (serviceEvents.isNotEmpty()) {
                        IconButton(
                            onClick = onClearEvents,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_events_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear logs",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    "All (${serviceEvents.size})" to 0,
                    "Detected ($detectedCount)" to 1,
                    "Accepted ($acceptedCount)" to 2,
                    "Filtered ($ignoredCount)" to 3
                )
                filters.forEach { (label, index) ->
                    val isSelected = selectedFilter == index
                    Surface(
                        onClick = { selectedFilter = index },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyanGlow else Slate950,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Cyan400.copy(alpha = 0.5f) else Slate800
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Cyan300 else Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Event list or Empty State
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate950)
                        .border(1.dp, Slate850, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "No events recorded yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        Text(
                            text = "When Rapido displays an order, 'Ride detected' & 'Order accepted' will appear here in real time.",
                            fontSize = 11.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onSimulateTest,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan300)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Sample Ride Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayedEvents.forEach { event ->
                        ServiceEventItemCard(event = event)
                    }

                    if (filteredEvents.size > 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Show less" else "Show ${filteredEvents.size - 5} more events",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Raw Console Log toggle footer
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRawConsole = !showRawConsole }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAW CONSOLE TELEMETRY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = if (showRawConsole) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedVisibility(visible = showRawConsole) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Text(
                            text = "> $rawServiceLog",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Cyan400,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceEventItemCard(event: ServiceEvent) {
    val timeFormatted = remember(event.timestamp) {
        try {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            sdf.format(Date(event.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    val (icon, iconBg, iconTint, badgeBg, badgeText) = when (event.type) {
        ServiceEventType.RIDE_DETECTED -> {
            Quint(Icons.Default.Radar, CyanGlow, Cyan400, CyanGlow, Cyan300)
        }
        ServiceEventType.ORDER_ACCEPTED -> {
            Quint(Icons.Default.CheckCircle, EmeraldGlow, Emerald400, EmeraldGlow, Emerald400)
        }
        ServiceEventType.ORDER_IGNORED -> {
            Quint(Icons.Default.Block, Color(0x2EEF4444), Rose400, Color(0x2EEF4444), Rose400)
        }
        ServiceEventType.ORDER_QUEUED -> {
            Quint(Icons.Default.HourglassTop, PurpleGlow, Purple400, PurpleGlow, Purple400)
        }
        ServiceEventType.MONITORING -> {
            Quint(Icons.Default.Bolt, Color(0x2E38BDF8), Color(0xFF38BDF8), Color(0x2E38BDF8), Color(0xFF38BDF8))
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Slate950,
        border = BorderStroke(1.dp, Slate850)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left icon container
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = event.title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate300
                )

                if (event.details.isNotBlank()) {
                    Text(
                        text = event.details,
                        fontSize = 10.sp,
                        color = Slate500,
                        maxLines = 1
                    )
                }
            }

            // Right badge chip
            if (event.badge.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeText.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = event.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabContent(
    isPassActive: Boolean,
    isWakeLockOn: Boolean,
    onWakeLockChange: (Boolean) -> Unit,
    isTtsOn: Boolean,
    onTtsChange: (Boolean) -> Unit,
    userNameInput: String,
    onUserNameChange: (String) -> Unit,
    ttsLanguage: String,
    onTtsLanguageChange: (String) -> Unit,
    onTestVoice: (String) -> Unit,
    isRapidoOnly: Boolean,
    onRapidoOnlyChange: (Boolean) -> Unit,
    isMasterSwitchOn: Boolean,
    onMasterSwitchChange: (Boolean) -> Unit,
    acceptDelayMs: Long,
    onAcceptDelayChange: (Long) -> Unit,
    isAccessibilityEnabled: Boolean,
    isNotificationListenerEnabled: Boolean = false,
    isBatteryOptimizationIgnored: Boolean,
    isOverlayAllowed: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenNotificationListener: () -> Unit = {},
    onOpenBatteryOptimization: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onUpdateAvailable: (GitHubUpdateManager.UpdateInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }

    val settingsViewModel: com.example.ui.SettingsViewModel = viewModel()
    val targetApps by settingsViewModel.targetApps.collectAsStateWithLifecycle()
    val keywords by settingsViewModel.keywords.collectAsStateWithLifecycle()

    var newKeywordText by remember { mutableStateOf("") }
    var newAppText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. SYSTEM PERMISSIONS CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("system_permissions_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Permissions",
                            tint = Emerald400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "System Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Required for reliable order detection & background execution",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                // 1. Accessibility Service
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Accessibility Service",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAccessibilityEnabled) EmeraldGlow else RoseGlow,
                                    border = BorderStroke(1.dp, if (isAccessibilityEnabled) Emerald500.copy(alpha = 0.5f) else Rose500.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isAccessibilityEnabled) "ENABLED" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAccessibilityEnabled) Emerald400 else Rose400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Inspects screen & auto-accepts orders",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenAccessibility,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) Slate800 else Emerald500,
                                contentColor = if (isAccessibilityEnabled) Slate300 else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("accessibility_perm_button")
                        ) {
                            Text(if (isAccessibilityEnabled) "Settings" else "Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Notification Listener Service
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notification Access",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isNotificationListenerEnabled) EmeraldGlow else RoseGlow,
                                    border = BorderStroke(1.dp, if (isNotificationListenerEnabled) Emerald500.copy(alpha = 0.5f) else Rose500.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isNotificationListenerEnabled) "ENABLED" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNotificationListenerEnabled) Emerald400 else Rose400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Captures incoming ride order notifications in real-time",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenNotificationListener,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNotificationListenerEnabled) Slate800 else Emerald500,
                                contentColor = if (isNotificationListenerEnabled) Slate300 else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("notification_listener_perm_button")
                        ) {
                            Text(if (isNotificationListenerEnabled) "Settings" else "Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Battery Optimization
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Battery Optimization",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isBatteryOptimizationIgnored) EmeraldGlow else AmberGlow,
                                    border = BorderStroke(1.dp, if (isBatteryOptimizationIgnored) Emerald500.copy(alpha = 0.5f) else Amber500.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isBatteryOptimizationIgnored) "UNRESTRICTED" else "RESTRICTED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBatteryOptimizationIgnored) Emerald400 else Amber400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Prevents OS from killing the background service",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenBatteryOptimization,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBatteryOptimizationIgnored) Slate800 else Amber500,
                                contentColor = if (isBatteryOptimizationIgnored) Slate300 else Slate950
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("battery_perm_button")
                        ) {
                            Text(if (isBatteryOptimizationIgnored) "Settings" else "Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Display Over Other Apps
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Display Over Other Apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isOverlayAllowed) EmeraldGlow else AmberGlow,
                                    border = BorderStroke(1.dp, if (isOverlayAllowed) Emerald500.copy(alpha = 0.5f) else Amber500.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isOverlayAllowed) "ALLOWED" else "DENIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverlayAllowed) Emerald400 else Amber400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enables floating overlay and status badge",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenOverlay,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayAllowed) Slate800 else Amber500,
                                contentColor = if (isOverlayAllowed) Slate300 else Slate950
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("overlay_perm_button")
                        ) {
                            Text(if (isOverlayAllowed) "Settings" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Refresh Permissions Button
                OutlinedButton(
                    onClick = onRefreshPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refresh_perms_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate800),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Permissions Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // =========================================================================
        // 2. AUTO ACCEPT SERVICE & DELAY CONFIGURATION
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auto_accept_settings_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(
                1.dp,
                if (isMasterSwitchOn) Emerald500.copy(alpha = 0.5f) else Slate800
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Auto Accept Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isMasterSwitchOn) EmeraldGlow else Color(0x33EF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Auto Accept Toggle",
                                tint = if (isMasterSwitchOn) Emerald400 else Rose400,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Auto Accept Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = if (isMasterSwitchOn) "Service Active & Scanning" else "Service Paused (Disabled)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isMasterSwitchOn) Emerald400 else Slate400
                            )
                        }
                    }

                    Switch(
                        checked = isMasterSwitchOn,
                        onCheckedChange = onMasterSwitchChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("auto_accept_settings_switch")
                    )
                }

                HorizontalDivider(color = Slate800, thickness = 1.dp)

                // Accept Wait Delay Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wait Delay Before Accept",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Cyan500.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (acceptDelayMs == 0L) "Instant (0ms)" else "${acceptDelayMs} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan300,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Adjust the pause duration between order arrival and automated click dispatch (0ms to 3000ms).",
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )

                    Slider(
                        value = acceptDelayMs.toFloat(),
                        onValueChange = { value ->
                            val rounded = (Math.round(value / 50.0) * 50).toLong()
                            onAcceptDelayChange(rounded)
                        },
                        valueRange = 0f..3000f,
                        steps = 59, // step of 50ms (3000 / 50 - 1)
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan400,
                            activeTrackColor = Cyan400,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accept_delay_slider")
                    )

                    // Quick delay presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0L to "Instant", 200L to "200ms", 500L to "500ms", 1000L to "1.0s", 2000L to "2.0s").forEach { (presetMs, label) ->
                            val isSelected = acceptDelayMs == presetMs
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Cyan500.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Cyan400 else Slate700
                                ),
                                onClick = { onAcceptDelayChange(presetMs) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preset_delay_${presetMs}ms")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Cyan300 else Slate400,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Firebase Remote Config Status Indicator
                    val remoteConfigStatus = RemoteConfigManager.lastFetchStatus.collectAsStateWithLifecycle().value
                    val dynamicPackages = RemoteConfigManager.allowedPackages.collectAsStateWithLifecycle().value
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth().testTag("remote_config_status_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Config: ${dynamicPackages.size} packages monitored (${remoteConfigStatus})",
                                fontSize = 10.sp,
                                color = Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. SCREEN WAKE & UNLOCK CARD (PREMIUM FEATURE)
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isPassActive) 1f else 0.5f)
                .testTag("screen_wake_lock_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(
                1.dp,
                if (isWakeLockOn && isPassActive) Cyan400.copy(alpha = 0.5f) else Slate800
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isWakeLockOn && isPassActive) Color(0x3322D3EE) else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Screen Wake Lock",
                                tint = if (isWakeLockOn && isPassActive) Cyan400 else Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Screen Wake & Unlock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = if (!isPassActive) "PREMIUM ONLY" else if (isWakeLockOn) "ACTIVE: WAKES ON ORDER" else "DISABLED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (!isPassActive) Amber400 else if (isWakeLockOn) Cyan400 else Slate400
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPassActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33F59E0B),
                                border = BorderStroke(1.dp, Color(0x66F59E0B))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Amber400,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PREMIUM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Amber400
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Switch(
                            checked = isWakeLockOn && isPassActive,
                            onCheckedChange = onWakeLockChange,
                            enabled = isPassActive,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Cyan500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800,
                                disabledUncheckedThumbColor = Slate600,
                                disabledUncheckedTrackColor = Slate850
                            ),
                            modifier = Modifier.testTag("wake_lock_toggle")
                        )
                    }
                }

                Text(
                    text = "Wakes the screen automatically for 5 seconds to accept orders even when the phone is locked.",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 17.sp
                )
            }
        }

        // =========================================================================
        // 3. VOICE ANNOUNCER (TTS) CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isPassActive) 1f else 0.5f)
                .testTag("tts_announcer_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isTtsOn && isPassActive) CyanGlow else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "TTS Icon",
                                tint = if (isTtsOn && isPassActive) Cyan400 else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Voice Announcer (TTS)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = if (!isPassActive) "PREMIUM ONLY"
                                else if (isTtsOn) {
                                    when (ttsLanguage) {
                                        "hi" -> "Speaks orders in Hindi"
                                        "gu" -> "Speaks orders in Gujarati"
                                        "mr" -> "Speaks orders in Marathi"
                                        else -> "Speaks orders in English"
                                    }
                                } else "Muted",
                                fontSize = 11.sp,
                                color = if (!isPassActive) Amber400 else if (isTtsOn) Cyan400 else Slate400
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPassActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33F59E0B),
                                border = BorderStroke(1.dp, Color(0x66F59E0B))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Amber400,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PREMIUM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Amber400
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Switch(
                            checked = isTtsOn && isPassActive,
                            onCheckedChange = onTtsChange,
                            enabled = isPassActive,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Cyan500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800,
                                disabledUncheckedThumbColor = Slate600,
                                disabledUncheckedTrackColor = Slate850
                            ),
                            modifier = Modifier.testTag("tts_toggle")
                        )
                    }
                }

                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = onUserNameChange,
                    enabled = isPassActive && isTtsOn,
                    label = { Text("Driver / Captain Name", color = Slate400) },
                    placeholder = { Text("e.g. Alex, Rahul, Captain", color = Slate500) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Slate400)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Multi-Language TTS Language Selector
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Announcement Language",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate300
                    )

                    val ttsLanguages = listOf(
                        "en" to "English",
                        "hi" to "Hindi (हिंदी)",
                        "gu" to "Gujarati (ગુજરાતી)",
                        "mr" to "Marathi (मराठी)"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ttsLanguages.forEach { (code, label) ->
                            FilterChip(
                                selected = ttsLanguage == code,
                                onClick = {
                                    onTtsLanguageChange(code)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (ttsLanguage == code) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                enabled = isPassActive && isTtsOn,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(alpha = 0.25f),
                                    selectedLabelColor = Cyan300,
                                    containerColor = Slate800,
                                    labelColor = Slate400,
                                    disabledContainerColor = Slate850,
                                    disabledLabelColor = Slate600
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (ttsLanguage == code) Cyan400 else Slate700,
                                    selectedBorderColor = Cyan400,
                                    enabled = isPassActive && isTtsOn,
                                    selected = ttsLanguage == code
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isPassActive) {
                            val name = userNameInput.ifBlank { "Captain" }
                            val sampleSpeech = AutoAcceptService.generateOrderAnnouncement(
                                context = context,
                                name = name,
                                price = 140,
                                distanceKm = 1.8f,
                                dropLocation = "Central Market"
                            )
                            onTestVoice(sampleSpeech)
                        }
                    },
                    enabled = isPassActive && isTtsOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_voice_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = Cyan300
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice Announcement", fontWeight = FontWeight.Bold)
                }
            }
        }

        // =========================================================================
        // 4. STRICT RAPIDO FILTER CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("strict_rapido_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Rapido Filter",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate50
                        )
                        Text(
                            text = if (isRapidoOnly) "Active on Rapido Captain (com.rapido.captain) only" else "Test Mode: Evaluates foreground app (safe: ignores self)",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                    Switch(
                        checked = isRapidoOnly,
                        onCheckedChange = onRapidoOnlyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }
            }
        }

        // =========================================================================
        // 5. APP UPDATES CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_updates_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "App Updates",
                                tint = Cyan400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "GitHub In-App Auto Updater",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Text(
                            text = "STABLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Version", fontSize = 12.sp, color = Slate400)
                            Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Repository", fontSize = 12.sp, color = Slate400)
                            Text(
                                "${GitHubUpdateManager.getOwner(context)}/${GitHubUpdateManager.getRepo(context)}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Cyan400
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCheckingUpdates = true
                            val result = GitHubUpdateManager.checkForUpdates(context)
                            isCheckingUpdates = false
                            when (result) {
                                is GitHubUpdateManager.UpdateCheckResult.UpdateAvailable -> {
                                    onUpdateAvailable(result.info)
                                }
                                is GitHubUpdateManager.UpdateCheckResult.UpToDate -> {
                                    Toast.makeText(context, "App is up to date! You are running the latest release.", Toast.LENGTH_SHORT).show()
                                }
                                is GitHubUpdateManager.UpdateCheckResult.Error -> {
                                    Toast.makeText(context, "Update check failed: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isCheckingUpdates,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan500,
                        contentColor = Slate950
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button")
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Slate950
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking GitHub Releases...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =========================================================================
        // 6. ADVANCED INTERACTION SETTINGS CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("advanced_settings_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Advanced Interaction Rules",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Target Ride Apps:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate300
                )
                Column {
                    for (app in targetApps) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = app.isEnabled,
                                onCheckedChange = { checked ->
                                    settingsViewModel.toggleApp(app.packageName, checked)
                                    AutoAcceptService.initCache(context)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                            )
                            Text(text = app.packageName, fontSize = 14.sp, color = Slate400, modifier = Modifier.weight(1f))
                            IconButton(onClick = { settingsViewModel.removeApp(app.packageName); AutoAcceptService.initCache(context) }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove App", tint = Slate500, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = newAppText,
                            onValueChange = { newAppText = it },
                            placeholder = { Text("Add new package name...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (newAppText.isNotBlank()) {
                                    settingsViewModel.addApp(newAppText.trim())
                                    newAppText = ""
                                    AutoAcceptService.initCache(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                        ) {
                            Text("Add")
                        }
                    }
                }

                Text(
                    text = "Auto-Accept Button Keywords:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate300,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Column {
                    for (kw in keywords) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = kw.isEnabled,
                                onCheckedChange = { checked ->
                                    settingsViewModel.toggleKeyword(kw.word, checked)
                                    AutoAcceptService.initCache(context)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                            )
                            Text(text = kw.word, fontSize = 14.sp, color = Slate400, modifier = Modifier.weight(1f))
                            IconButton(onClick = { settingsViewModel.removeKeyword(kw.word); AutoAcceptService.initCache(context) }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Keyword", tint = Slate500, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = newKeywordText,
                            onValueChange = { newKeywordText = it },
                            placeholder = { Text("Add new keyword...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (newKeywordText.isNotBlank()) {
                                    settingsViewModel.addKeyword(newKeywordText.trim())
                                    newKeywordText = ""
                                    AutoAcceptService.initCache(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}



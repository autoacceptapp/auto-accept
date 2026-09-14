package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay

/**
 * ServiceStatusDashboardWidget
 *
 * A high-visibility dashboard widget that monitors the status of:
 * 1. AccessibilityService (AutoAcceptService) -> 'Active' or 'Inactive'
 * 2. NotificationListenerService (RideNotificationService) -> 'Active' or 'Inactive'
 *
 * Provides quick-links to system settings to toggle each service,
 * real-time status updates on lifecycle resume, and home screen launcher widget pinning.
 */
@Composable
fun ServiceStatusDashboardWidget(
    modifier: Modifier = Modifier,
    onNavigateToDebugLogs: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live state from services
    val isAccServiceRunning by AutoAcceptService.isServiceRunning.collectAsState()
    val isNotifListenerConnected by RideNotificationService.isListenerConnected.collectAsState()

    // Query system settings states
    var isAccSettingsEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, AutoAcceptService::class.java))
    }
    var isNotifSettingsEnabled by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }
    var showAdbDialog by remember { mutableStateOf(false) }

    val isAccessibilityActive = isAccServiceRunning || isAccSettingsEnabled
    val isNotificationListenerActive = isNotifListenerConnected || isNotifSettingsEnabled

    if (showAdbDialog) {
        DirectAccessibilitySetupDialog(
            context = context,
            onDismiss = { showAdbDialog = false },
            onOpenSettings = {
                openAccessibilitySettings(context)
                Toast.makeText(context, "Find 'Auto Accept' and toggle ON", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun refreshStates() {
        isAccSettingsEnabled = isAccessibilityServiceEnabled(context, AutoAcceptService::class.java)
        isNotifSettingsEnabled = isNotificationListenerEnabled(context)
        ServiceStatusNotificationManager.updateStatus(context)
        ServiceStatusWidgetProvider.updateAllWidgets(context)
    }

    // Auto refresh whenever returning from system settings (e.g. user toggles the switch and taps back)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic safety check every 3 seconds while widget is active
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            refreshStates()
        }
    }

    val allServicesActive = isAccessibilityActive && isNotificationListenerActive

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_services_widget"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(
            1.dp,
            when {
                allServicesActive -> Emerald500.copy(alpha = 0.5f)
                isAccessibilityActive || isNotificationListenerActive -> Amber500.copy(alpha = 0.5f)
                else -> Rose500.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -----------------------------------------------------------------
            // Widget Header Row
            // -----------------------------------------------------------------
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
                            .background(
                                when {
                                    allServicesActive -> EmeraldGlow
                                    isAccessibilityActive || isNotificationListenerActive -> AmberGlow
                                    else -> Color(0x33F43F5E)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                allServicesActive -> Icons.Default.CheckCircle
                                else -> Icons.Default.Warning
                            },
                            contentDescription = "Service Health Status",
                            tint = when {
                                allServicesActive -> Emerald400
                                isAccessibilityActive || isNotificationListenerActive -> Amber400
                                else -> Rose400
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Service Status Monitor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = when {
                                allServicesActive -> "ALL SERVICES ACTIVE"
                                isAccessibilityActive -> "NOTIFICATION ACCESS INACTIVE"
                                isNotificationListenerActive -> "ACCESSIBILITY INACTIVE"
                                else -> "SERVICES INACTIVE"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                allServicesActive -> Emerald400
                                isAccessibilityActive || isNotificationListenerActive -> Amber400
                                else -> Rose400
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Manual Refresh Button
                    IconButton(
                        onClick = {
                            refreshStates()
                            Toast.makeText(context, "Services status refreshed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).testTag("btn_refresh_services_widget")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Services Status",
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Optional Pin to Home Screen button (Android 8.0+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                            IconButton(
                                onClick = {
                                    val myProvider = ComponentName(context, ServiceStatusWidgetProvider::class.java)
                                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    Toast.makeText(context, "Pin widget to your Home screen", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp).testTag("btn_pin_home_widget")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = "Pin to Home Screen",
                                    tint = Slate300,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Service 1: Accessibility Service Monitor Item
            // -----------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Slate950,
                border = BorderStroke(
                    1.dp,
                    if (isAccessibilityActive) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("widget_accessibility_item")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityActive) EmeraldGlow else Color(0x33F43F5E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = if (isAccessibilityActive) Emerald400 else Rose400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Accessibility Service",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Status Badge (Active / Inactive)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAccessibilityActive) EmeraldGlow else Color(0x26F43F5E),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAccessibilityActive) Emerald500.copy(alpha = 0.6f) else Rose500.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("widget_accessibility_status")
                                ) {
                                    Text(
                                        text = if (isAccessibilityActive) "Active" else "Inactive",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAccessibilityActive) Emerald400 else Rose400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Auto-scans screen & clicks Accept",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Dedicated in-app switch for Accessibility Service
                        Switch(
                            checked = isAccessibilityActive,
                            onCheckedChange = { targetState ->
                                if (!targetState) {
                                    val stopped = disableAccessibilityServiceDirectly(context)
                                    refreshStates()
                                    Toast.makeText(
                                        context,
                                        if (stopped) "Accessibility Service stopped directly"
                                        else "Accessibility stopped",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val enabled = enableAccessibilityServiceDirectly(context)
                                    if (enabled) {
                                        refreshStates()
                                        Toast.makeText(context, "Accessibility Service enabled directly!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showAdbDialog = true
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Emerald500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            ),
                            modifier = Modifier.testTag("widget_accessibility_toggle_switch")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Quick-link button to toggle in settings if desired
                        IconButton(
                            onClick = {
                                openAccessibilitySettings(context)
                                Toast.makeText(context, "Find 'Auto Accept' and toggle ON", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_accessibility_settings")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Settings",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Service 2: Notification Listener Service Monitor Item
            // -----------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Slate950,
                border = BorderStroke(
                    1.dp,
                    if (isNotificationListenerActive) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("widget_notification_item")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isNotificationListenerActive) EmeraldGlow else Color(0x33F43F5E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isNotificationListenerActive) Emerald400 else Rose400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notification Listener",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Status Badge (Active / Inactive)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isNotificationListenerActive) EmeraldGlow else Color(0x26F43F5E),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isNotificationListenerActive) Emerald500.copy(alpha = 0.6f) else Rose500.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("widget_notification_status")
                                ) {
                                    Text(
                                        text = if (isNotificationListenerActive) "Active" else "Inactive",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isNotificationListenerActive) Emerald400 else Rose400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Captures orders when app is in background",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick-link button to toggle in settings
                    Button(
                        onClick = {
                            openNotificationListenerSettings(context)
                            Toast.makeText(context, "Find 'Auto Accept' and allow notification access", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNotificationListenerActive) Slate800 else Rose500,
                            contentColor = if (isNotificationListenerActive) Cyan400 else Slate50
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_notification_settings")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isNotificationListenerActive) "Settings" else "Toggle ON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Settings",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Dynamic Guidance Notice
            // -----------------------------------------------------------------
            if (!allServicesActive) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x1AF59E0B),
                    border = BorderStroke(1.dp, Color(0x4DF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Amber400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!isAccessibilityActive && !isNotificationListenerActive) {
                                "Both services must be Active to auto-detect and accept incoming orders. Tap 'Toggle ON' to enable each in system settings."
                            } else if (!isAccessibilityActive) {
                                "Accessibility Service must be Active to automatically tap the 'Accept' button on your screen."
                            } else {
                                "Notification Listener is recommended to detect orders when Rapido is minimized or your screen is in another app."
                            },
                            fontSize = 11.sp,
                            color = Slate200,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x1A10B981),
                    border = BorderStroke(1.dp, Color(0x4D10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Both services are active and operational. Ready to capture and auto-accept incoming rides.",
                            fontSize = 11.sp,
                            color = Slate200,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

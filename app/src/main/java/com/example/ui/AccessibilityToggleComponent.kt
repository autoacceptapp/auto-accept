package com.example.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AutoAcceptService
import com.example.DirectAccessibilitySetupDialog
import com.example.canWriteSecureSettings
import com.example.disableAccessibilityServiceDirectly
import com.example.enableAccessibilityServiceDirectly
import com.example.isAccessibilityServiceEnabled
import com.example.openAccessibilitySettings
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Accessibility Service Toggle Card
 * Allows drivers to easily enable or disable the AccessibilityService directly from the UI
 * without navigating to system settings every time.
 */
@Composable
fun AccessibilityServiceToggleCard(
    isServiceEnabled: Boolean,
    onServiceStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAdbDialog by remember { mutableStateOf(false) }
    val isSecureSettingsGranted = remember(isServiceEnabled) { canWriteSecureSettings(context) }
    val isLiveConnected by AutoAcceptService.isServiceRunning.collectAsState()

    val isActive = isServiceEnabled || isLiveConnected

    if (showAdbDialog) {
        DirectAccessibilitySetupDialog(
            context = context,
            onDismiss = { showAdbDialog = false },
            onOpenSettings = {
                openAccessibilitySettings(context)
                Toast.makeText(context, "Locate 'Auto Accept' and toggle ON", Toast.LENGTH_LONG).show()
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("accessibility_service_toggle_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(
            1.dp,
            if (isActive) Emerald500.copy(alpha = 0.5f) else Slate800
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Icon, Title, Status Badges & Dedicated Switch
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
                            .background(if (isActive) EmeraldGlow else Color(0x33EF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = "Accessibility Service",
                            tint = if (isActive) Emerald400 else Rose400,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Accessibility Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isActive) EmeraldGlow else Color(0x26F43F5E),
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) Emerald500.copy(alpha = 0.6f) else Rose500.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = if (isActive) "ACTIVE" else "OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isActive) Emerald400 else Rose400,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isActive) {
                                "Direct in-app toggle: instant shutoff & background scan"
                            } else {
                                "Turn ON directly or tap for 1-click ADB mode"
                            },
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                // Dedicated M3 Switch for Accessibility Service
                Switch(
                    checked = isActive,
                    onCheckedChange = { targetState ->
                        if (!targetState) {
                            // Turning OFF: directly disable without settings navigation
                            val stopped = disableAccessibilityServiceDirectly(context)
                            onServiceStateChanged(false)
                            Toast.makeText(
                                context,
                                if (stopped) "Accessibility Service stopped directly (no settings needed)"
                                else "Accessibility Service stopped",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Turning ON: try direct programmatic activation first
                            val enabled = enableAccessibilityServiceDirectly(context)
                            if (enabled) {
                                onServiceStateChanged(true)
                                Toast.makeText(
                                    context,
                                    "Accessibility Service enabled directly!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Direct activation requires secure setting permission, prompt helper dialog
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
                    modifier = Modifier.testTag("accessibility_service_toggle_switch")
                )
            }

            // Direct Mode & Capabilities Info Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate950,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isSecureSettingsGranted) Icons.Default.CheckCircle else Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (isSecureSettingsGranted) Emerald400 else Cyan400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSecureSettingsGranted) {
                                "Direct Toggle: Fully Unlocked (1-Click ON/OFF)"
                            } else {
                                "Direct OFF Active • 1-Click ON via ADB"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSecureSettingsGranted) Emerald400 else Slate300
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Info/ADB Dialog Button
                        IconButton(
                            onClick = { showAdbDialog = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("btn_accessibility_direct_info")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Direct Mode Setup",
                                tint = Cyan400,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Fallback Open Settings Button
                        IconButton(
                            onClick = {
                                openAccessibilitySettings(context)
                                Toast.makeText(context, "Opening Accessibility Settings...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("btn_accessibility_open_settings_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open System Settings",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Accessibility Service Row for the Settings Tab & Widget
 */
@Composable
fun AccessibilityServiceToggleRow(
    isServiceEnabled: Boolean,
    onServiceStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAdbDialog by remember { mutableStateOf(false) }
    val isLiveConnected by AutoAcceptService.isServiceRunning.collectAsState()
    val isActive = isServiceEnabled || isLiveConnected

    if (showAdbDialog) {
        DirectAccessibilitySetupDialog(
            context = context,
            onDismiss = { showAdbDialog = false },
            onOpenSettings = {
                openAccessibilitySettings(context)
                Toast.makeText(context, "Locate 'Auto Accept' and toggle ON", Toast.LENGTH_LONG).show()
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate950,
        border = BorderStroke(1.dp, if (isActive) Emerald500.copy(alpha = 0.4f) else Slate800),
        modifier = modifier.fillMaxWidth()
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
                        color = if (isActive) EmeraldGlow else Color(0x26F43F5E),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) Emerald500.copy(alpha = 0.5f) else Rose500.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isActive) "ENABLED" else "DISABLED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Emerald400 else Rose400,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Direct in-app switch • No settings navigation needed to turn OFF",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dedicated switch for Settings Tab
                Switch(
                    checked = isActive,
                    onCheckedChange = { targetState ->
                        if (!targetState) {
                            val stopped = disableAccessibilityServiceDirectly(context)
                            onServiceStateChanged(false)
                            Toast.makeText(
                                context,
                                if (stopped) "Accessibility Service disabled directly" else "Accessibility stopped",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val enabled = enableAccessibilityServiceDirectly(context)
                            if (enabled) {
                                onServiceStateChanged(true)
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
                    modifier = Modifier.testTag("settings_accessibility_toggle_switch")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Shortcut to open Android system settings if needed
                IconButton(
                    onClick = {
                        openAccessibilitySettings(context)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_open_accessibility_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "System Settings",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

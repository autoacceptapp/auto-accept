package com.example

import android.annotation.SuppressLint
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
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

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    if (enabledServices != null) {
        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo?.serviceInfo
            if (serviceInfo != null &&
                serviceInfo.packageName == context.packageName &&
                serviceInfo.name == serviceClass.name
            ) {
                return true
            }
        }
    }

    val expectedName = ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)

    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null &&
            enabledComponent.packageName == expectedName.packageName &&
            (enabledComponent.className == expectedName.className ||
             enabledComponent.shortClassName == expectedName.shortClassName)
        ) {
            return true
        }
        if (componentNameString.equals(expectedName.flattenToString(), ignoreCase = true)) {
            return true
        }
    }
    return false
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

/**
 * Checks if the app has been granted WRITE_SECURE_SETTINGS permission
 * (e.g. via ADB: adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS or root).
 */
fun canWriteSecureSettings(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Returns the exact ADB command line string drivers can run on PC/Mac to unlock 1-click direct toggle.
 */
fun getAdbCommandForDirectToggle(context: Context): String {
    return "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
}

/**
 * Attempts to programmatically enable the AccessibilityService directly without navigating to system settings.
 * 1. If WRITE_SECURE_SETTINGS is granted, modifies Settings.Secure directly.
 * 2. If rooted, executes via su shell.
 * Returns true if successfully enabled directly, false if manual system settings navigation or ADB grant is required.
 */
fun enableAccessibilityServiceDirectly(context: Context): Boolean {
    val expectedName = ComponentName(context, AutoAcceptService::class.java).flattenToString()
    val cr = context.contentResolver

    // Method 1: Check WRITE_SECURE_SETTINGS
    if (canWriteSecureSettings(context)) {
        try {
            val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val services = current.split(":").filter { it.isNotBlank() }.toMutableSet()
            services.add(expectedName)
            val newSetting = services.joinToString(":")
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newSetting)
            Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
            Log.i("PermissionsHelper", "Enabled AccessibilityService directly via WRITE_SECURE_SETTINGS")
            return true
        } catch (e: Exception) {
            Log.e("PermissionsHelper", "Failed to write secure settings: ${e.message}")
        }
    }

    // Method 2: Try root execution if device has su
    try {
        val process = Runtime.getRuntime().exec("su")
        val os = java.io.DataOutputStream(process.outputStream)
        val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val services = current.split(":").filter { it.isNotBlank() }.toMutableSet()
        services.add(expectedName)
        val newSetting = services.joinToString(":")
        os.writeBytes("settings put secure enabled_accessibility_services $newSetting\n")
        os.writeBytes("settings put secure accessibility_enabled 1\n")
        os.writeBytes("exit\n")
        os.flush()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            Log.i("PermissionsHelper", "Enabled AccessibilityService directly via root su")
            return true
        }
    } catch (_: Exception) {
        // Device is not rooted or su denied
    }

    return false
}

/**
 * Programmatically disables the AccessibilityService directly without navigating to system settings.
 * 1. Uses Android's native disableSelf() on the active service instance (works on Android 7.0+ with zero permissions).
 * 2. If WRITE_SECURE_SETTINGS or root is available, also cleans it from system settings.
 * Returns true if successfully stopped.
 */
fun disableAccessibilityServiceDirectly(context: Context): Boolean {
    // 1. First trigger native disableSelf() on running service instance
    val nativeSuccess = AutoAcceptService.disableService()

    val expectedName = ComponentName(context, AutoAcceptService::class.java).flattenToString()
    val cr = context.contentResolver

    // 2. If WRITE_SECURE_SETTINGS is available, remove from settings as well
    if (canWriteSecureSettings(context)) {
        try {
            val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val services = current.split(":").filter { it.isNotBlank() && it != expectedName }
            val newSetting = services.joinToString(":")
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newSetting)
            if (services.isEmpty()) {
                Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_ENABLED, "0")
            }
            Log.i("PermissionsHelper", "Disabled AccessibilityService directly via WRITE_SECURE_SETTINGS")
            return true
        } catch (e: Exception) {
            Log.e("PermissionsHelper", "Failed to disable via secure settings: ${e.message}")
        }
    }

    // 3. Try root if available
    try {
        val process = Runtime.getRuntime().exec("su")
        val os = java.io.DataOutputStream(process.outputStream)
        val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val remaining = current.split(":").filter { it.isNotBlank() && it != expectedName }.joinToString(":")
        os.writeBytes("settings put secure enabled_accessibility_services $remaining\n")
        os.writeBytes("exit\n")
        os.flush()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            return true
        }
    } catch (_: Exception) {
        // Not rooted
    }

    return nativeSuccess
}

/**
 * Sleek Dialog that explains the 1-Click Direct Toggle feature and lets drivers copy
 * the one-line ADB command or quickly open System Settings to enable the service.
 */
@Composable
fun DirectAccessibilitySetupDialog(
    context: Context,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val adbCmd = remember { getAdbCommandForDirectToggle(context) }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Direct Service Toggle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                    Text(
                        text = "No System Settings Navigation",
                        fontSize = 11.sp,
                        color = Emerald400,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You can turn OFF the Accessibility Service instantly with 1 tap from the switch (no settings required).",
                    fontSize = 13.sp,
                    color = Slate300,
                    lineHeight = 18.sp
                )
                Text(
                    text = "To also turn it ON directly without opening Android Settings every time, grant the 1-time secure permission via computer/ADB:",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 17.sp
                )

                // ADB Command Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = adbCmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Cyan400,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ADB Command", adbCmd)
                                clipboard.setPrimaryClip(clip)
                                copied = true
                                Toast.makeText(context, "ADB command copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copied) Emerald500 else Slate800,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_copy_adb_command")
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (copied) "Copied" else "Copy", fontSize = 11.sp)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x1A06B6D4),
                    border = BorderStroke(1.dp, Color(0x3306B6D4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Pro-Tip: You can also keep Accessibility ON and simply use the 'Master Auto-Accept' switch on the home screen to pause/resume anytime without opening settings!",
                        fontSize = 11.sp,
                        color = Slate300,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onOpenSettings()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_dialog_open_accessibility")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Settings This Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = Slate400)
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp)
    )
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == context.packageName) {
                return true
            }
        }
    }
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open notification listener settings", Toast.LENGTH_SHORT).show()
    }
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
}

@SuppressLint("BatteryLife")
fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open battery settings", Toast.LENGTH_SHORT).show()
        }
    }
}

fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Opens OEM-specific Auto-Start permissions (MIUI/HyperOS, ColorOS, OxygenOS, FuntouchOS, etc.)
 * with safe fallback to application details settings.
 */
fun openAutoStartSettings(context: Context) {
    val autoStartIntents = listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartActivity")),
        Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
        Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity")),
        Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")),
        Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListAct"))
    )

    for (intent in autoStartIntents) {
        try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next OEM intent
        }
    }

    // Fallback to application details settings
    try {
        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallbackIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open auto-start settings", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MissingPermissionsDialog(
    isAccessibilityEnabled: Boolean,
    isOverlayAllowed: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33F43F5E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Rose400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                    Text(
                        text = "Enable required permissions to auto-accept",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "The following permissions are required for the background clicker to detect and accept orders:",
                    fontSize = 12.sp,
                    color = Slate300,
                    lineHeight = 16.sp
                )

                if (!isAccessibilityEnabled) {
                    MissingPermissionRow(
                        title = "Accessibility Service",
                        subtitle = "Required to detect and click Rapido orders",
                        buttonText = "Enable",
                        onClick = onOpenAccessibility
                    )
                }

                if (!isOverlayAllowed) {
                    MissingPermissionRow(
                        title = "Display Over Other Apps",
                        subtitle = "Required to interact with floating order banners",
                        buttonText = "Grant",
                        onClick = onOpenOverlay
                    )
                }

                if (!isBatteryOptimizationIgnored) {
                    MissingPermissionRow(
                        title = "Battery Optimization",
                        subtitle = "Prevent Android from killing the background service",
                        buttonText = "Unrestrict",
                        onClick = onOpenBatteryOptimization
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
            ) {
                Text("Dismiss", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun MissingPermissionRow(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
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
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Slate400,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

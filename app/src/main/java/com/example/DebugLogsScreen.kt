package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

private enum class LogFilterType {
    ALL,
    MISSED_ONLY,
    ACCESSIBILITY_ONLY,
    NOTIFICATION_ONLY
}

/**
 * Dedicated UI Screen displaying the last 50 debug logs from AccessibilityService
 * and RideNotificationService in a scrolling list to diagnose and troubleshoot missed orders.
 */
@Composable
fun DebugLogsScreen(
    isAccessibilityEnabled: Boolean,
    isNotificationListenerEnabled: Boolean = false,
    onOpenAccessibility: () -> Unit = {},
    onOpenNotificationListener: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawLogs by DebugLogManager.logs.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var showHelpGuide by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(rawLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilterType.ALL -> rawLogs
            LogFilterType.MISSED_ONLY -> rawLogs.filter { it.isMissedOrder }
            LogFilterType.ACCESSIBILITY_ONLY -> rawLogs.filter { it.serviceOrigin == ServiceOrigin.ACCESSIBILITY }
            LogFilterType.NOTIFICATION_ONLY -> rawLogs.filter { it.serviceOrigin == ServiceOrigin.NOTIFICATION }
        }
    }

    val missedCount = remember(rawLogs) { rawLogs.count { it.isMissedOrder } }
    val accessibilityCount = remember(rawLogs) { rawLogs.count { it.serviceOrigin == ServiceOrigin.ACCESSIBILITY } }
    val notificationCount = remember(rawLogs) { rawLogs.count { it.serviceOrigin == ServiceOrigin.NOTIFICATION } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("debug_logs_screen")
    ) {
        // ---------------------------------------------------------------------
        // 1. TOP HEADER & SERVICE STATUS BAR
        // ---------------------------------------------------------------------
        Surface(
            color = Slate900,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Cyan500.copy(alpha = 0.2f))
                                .border(1.dp, Cyan400.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug Icon",
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Order Debugger",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "Last 50 Service & Notification Logs",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Action: Export & Share CSV
                        IconButton(
                            onClick = {
                                if (rawLogs.isEmpty()) {
                                    Toast.makeText(context, "No logs to export", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                if (selectedFilter != LogFilterType.ALL && filteredLogs.size < rawLogs.size) {
                                    showExportCsvDialog = true
                                } else {
                                    val result = DebugLogManager.shareLogsAsCsv(context, filteredLogs)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Exporting ${filteredLogs.size} logs to CSV...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Export error: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_export_csv_logs")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export and share CSV logs",
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Action: Copy logs to clipboard
                        IconButton(
                            onClick = {
                                val text = DebugLogManager.exportLogsToPlainText()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Rapido AutoAccept Debug Logs", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${rawLogs.size} logs to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_copy_debug_logs")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy all logs",
                                tint = Slate200,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Action: Clear logs
                        IconButton(
                            onClick = {
                                DebugLogManager.clearLogs()
                                Toast.makeText(context, "Debug logs cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_clear_debug_logs")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear logs",
                                tint = Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnostic Status Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accessibility Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAccessibilityEnabled) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isAccessibilityEnabled) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f)),
                        onClick = onOpenAccessibility,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_pill_accessibility")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccessibilityEnabled) Emerald400 else Rose400)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAccessibilityEnabled) "Accessibility: Active" else "Accessibility: OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAccessibilityEnabled) Emerald400 else Rose400
                            )
                        }
                    }

                    // Notification Listener Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNotificationListenerEnabled) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isNotificationListenerEnabled) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f)),
                        onClick = onOpenNotificationListener,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_pill_notification_listener")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isNotificationListenerEnabled) Emerald400 else Rose400)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNotificationListenerEnabled) "Listener: Active" else "Listener: OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNotificationListenerEnabled) Emerald400 else Rose400
                            )
                        }
                    }

                    // Missed Orders Counter Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (missedCount > 0) Rose500.copy(alpha = 0.2f) else Slate850,
                        border = BorderStroke(1.dp, if (missedCount > 0) Rose500.copy(alpha = 0.5f) else Slate800),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$missedCount Missed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (missedCount > 0) Rose400 else Slate400
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 2. FILTER CHIPS & SIMULATION BAR
        // ---------------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate950)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == LogFilterType.ALL,
                    onClick = { selectedFilter = LogFilterType.ALL },
                    label = { Text("All (${rawLogs.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate800,
                        selectedLabelColor = Slate50,
                        containerColor = Slate950,
                        labelColor = Slate400
                    ),
                    border = BorderStroke(1.dp, if (selectedFilter == LogFilterType.ALL) Cyan400 else Slate800),
                    modifier = Modifier.testTag("chip_filter_all")
                )

                FilterChip(
                    selected = selectedFilter == LogFilterType.MISSED_ONLY,
                    onClick = { selectedFilter = LogFilterType.MISSED_ONLY },
                    label = { Text("⚠️ Missed ($missedCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Rose500.copy(alpha = 0.25f),
                        selectedLabelColor = Rose400,
                        containerColor = Slate950,
                        labelColor = if (missedCount > 0) Rose400 else Slate500
                    ),
                    border = BorderStroke(1.dp, if (selectedFilter == LogFilterType.MISSED_ONLY) Rose500 else Slate800),
                    modifier = Modifier.testTag("chip_filter_missed")
                )

                FilterChip(
                    selected = selectedFilter == LogFilterType.ACCESSIBILITY_ONLY,
                    onClick = { selectedFilter = LogFilterType.ACCESSIBILITY_ONLY },
                    label = { Text("Accessibility ($accessibilityCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate800,
                        selectedLabelColor = Slate50,
                        containerColor = Slate950,
                        labelColor = Slate400
                    ),
                    border = BorderStroke(1.dp, if (selectedFilter == LogFilterType.ACCESSIBILITY_ONLY) Cyan400 else Slate800),
                    modifier = Modifier.testTag("chip_filter_accessibility")
                )

                FilterChip(
                    selected = selectedFilter == LogFilterType.NOTIFICATION_ONLY,
                    onClick = { selectedFilter = LogFilterType.NOTIFICATION_ONLY },
                    label = { Text("Notify ($notificationCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate800,
                        selectedLabelColor = Slate50,
                        containerColor = Slate950,
                        labelColor = Slate400
                    ),
                    border = BorderStroke(1.dp, if (selectedFilter == LogFilterType.NOTIFICATION_ONLY) Amber400 else Slate800),
                    modifier = Modifier.testTag("chip_filter_notification")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Simulation & Help Trigger Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Troubleshooting Guide Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showHelpGuide = !showHelpGuide }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help Guide",
                        tint = Cyan400,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showHelpGuide) "Hide Diagnostic Tips" else "Why Are Orders Missed?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Cyan400
                    )
                    Icon(
                        imageVector = if (showHelpGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Export CSV Pill Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Cyan500.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (rawLogs.isEmpty()) {
                                    Toast.makeText(context, "No logs to export", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                if (selectedFilter != LogFilterType.ALL && filteredLogs.size < rawLogs.size) {
                                    showExportCsvDialog = true
                                } else {
                                    val result = DebugLogManager.shareLogsAsCsv(context, filteredLogs)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Exporting ${filteredLogs.size} logs to CSV...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Export error: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .testTag("btn_export_csv_action")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export CSV",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. COLLAPSIBLE TROUBLESHOOTING GUIDE
        // ---------------------------------------------------------------------
        AnimatedVisibility(
            visible = showHelpGuide,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🛠️ Common Missed Order Causes & Fixes:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    DiagnosticTipItem(
                        number = "1",
                        title = "Distance / Price Filters Rejection",
                        explanation = "Check if the ride's pickup distance exceeds your 'Max Distance' or if fare was below 'Min Price'. Check individual filter toggles in the Home tab."
                    )
                    DiagnosticTipItem(
                        number = "2",
                        title = "Blacklisted Keywords",
                        explanation = "If drop or pickup addresses contain words like 'Airport', 'Toll', etc., the order is rejected. Edit your Blacklist in Home tab."
                    )
                    DiagnosticTipItem(
                        number = "3",
                        title = "Accept Button Expired (Delay Too Long)",
                        explanation = "If another captain accepted the ride first, the accept button disappears. Reduce your Accept Delay slider (e.g. 150ms-250ms)."
                    )
                    DiagnosticTipItem(
                        number = "4",
                        title = "Notification Listener Blocked",
                        explanation = "Ensure Notification Access is granted to Rapido Auto Accept in device settings so incoming orders immediately trigger the scanner."
                    )
                }
            }
        }

        HorizontalDivider(color = Slate900, thickness = 1.dp)

        // ---------------------------------------------------------------------
        // 4. SCROLLING LIST OF LAST 50 LOGS
        // ---------------------------------------------------------------------
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Slate900),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No logs match this filter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate300
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Logs from AccessibilityService and RideNotificationService appear here automatically as rides arrive.",
                        fontSize = 11.sp,
                        color = Slate500,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("debug_logs_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logItem ->
                    DebugLogCard(entry = logItem)
                }

                item {
                    // Export CSV footer card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share Diagnostic Logs (CSV)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                                Text(
                                    text = "Export ${filteredLogs.size} logs to email, messaging, or spreadsheet apps",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (selectedFilter != LogFilterType.ALL && filteredLogs.size < rawLogs.size) {
                                        showExportCsvDialog = true
                                    } else {
                                        val res = DebugLogManager.shareLogsAsCsv(context, filteredLogs)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Exporting ${filteredLogs.size} logs to CSV...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_export_csv_list_footer")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 5. EXPORT CSV DIALOG (CHOOSER: FILTERED VS ALL)
        // ---------------------------------------------------------------------
        if (showExportCsvDialog) {
            AlertDialog(
                onDismissRequest = { showExportCsvDialog = false },
                containerColor = Slate900,
                iconContentColor = Cyan400,
                titleContentColor = Slate50,
                textContentColor = Slate300,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Cyan500.copy(alpha = 0.2f))
                            .border(1.dp, Cyan400.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = Cyan400,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Export Debug Logs (CSV)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Slate50
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Share diagnostic debugging logs formatted as an RFC 4180 CSV spreadsheet compatible with Excel, Google Sheets, Gmail, WhatsApp, and messaging apps.",
                            fontSize = 12.sp,
                            color = Slate300,
                            lineHeight = 17.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate950,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Included CSV Columns:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Text(
                                    text = "• Formatted Timestamp & Epoch Millis\n• Service Origin (Accessibility / Notification)\n• Order Category & Severity Level\n• Fare (₹), Distance (km), Pickup & Drop\n• Missed Order Flag, Reason & Fix Recommendation\n• Technical Details & Screen Hierarchy Info",
                                    fontSize = 10.sp,
                                    color = Slate300,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Text(
                            text = "Select logs to export:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400
                        )

                        // Option A: Export filtered logs
                        Button(
                            onClick = {
                                showExportCsvDialog = false
                                val res = DebugLogManager.shareLogsAsCsv(
                                    context,
                                    filteredLogs,
                                    "Rapido Auto Accept - Filtered Debug Logs (${filteredLogs.size} events)"
                                )
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Exporting ${filteredLogs.size} filtered logs...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_csv_filtered_option"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950)
                        ) {
                            Text(
                                text = "Export Filtered (${filteredLogs.size} logs)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Option B: Export all logs
                        OutlinedButton(
                            onClick = {
                                showExportCsvDialog = false
                                val res = DebugLogManager.shareLogsAsCsv(
                                    context,
                                    rawLogs,
                                    "Rapido Auto Accept - All Debug Logs (${rawLogs.size} events)"
                                )
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Exporting all ${rawLogs.size} logs...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_csv_all_option"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200)
                        ) {
                            Text(
                                text = "Export All Logs (${rawLogs.size} logs)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showExportCsvDialog = false },
                        modifier = Modifier.testTag("btn_cancel_export_csv")
                    ) {
                        Text("Cancel", color = Slate400, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

/**
 * Individual Log Card with visual hierarchy, severity indicators,
 * and high-contrast diagnostic warning callout for missed orders.
 */
@Composable
private fun DebugLogCard(entry: DebugLogEntry) {
    var isExpanded by remember { mutableStateOf(false) }

    val borderColor = when {
        entry.isMissedOrder -> Rose500.copy(alpha = 0.6f)
        entry.severity == LogSeverity.SUCCESS -> Emerald500.copy(alpha = 0.6f)
        entry.severity == LogSeverity.WARNING -> Amber400.copy(alpha = 0.5f)
        else -> Slate800
    }

    val containerColor = when {
        entry.isMissedOrder -> Color(0xFF1E1318) // subtle deep rose tint
        entry.severity == LogSeverity.SUCCESS -> Color(0xFF0F1E19) // subtle deep emerald tint
        else -> Slate900
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("debug_log_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Time, Service Origin, and Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Origin Badge
                    val (originBg, originText, originIcon) = when (entry.serviceOrigin) {
                        ServiceOrigin.ACCESSIBILITY -> Triple(
                            Cyan500.copy(alpha = 0.2f),
                            Cyan400,
                            Icons.Default.FlashOn
                        )
                        ServiceOrigin.NOTIFICATION -> Triple(
                            Amber400.copy(alpha = 0.2f),
                            Amber400,
                            Icons.Default.Notifications
                        )
                        ServiceOrigin.SYSTEM -> Triple(
                            Slate800,
                            Slate300,
                            Icons.Default.Info
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = originBg,
                        border = BorderStroke(1.dp, originText.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = originIcon,
                                contentDescription = null,
                                tint = originText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (entry.serviceOrigin) {
                                    ServiceOrigin.ACCESSIBILITY -> "ACCESSIBILITY"
                                    ServiceOrigin.NOTIFICATION -> "NOTIFICATION"
                                    ServiceOrigin.SYSTEM -> "SYSTEM"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = originText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Severity Badge
                    val (sevBg, sevText, sevLabel) = when {
                        entry.isMissedOrder -> Triple(Rose500.copy(alpha = 0.25f), Rose400, "MISSED")
                        entry.severity == LogSeverity.SUCCESS -> Triple(Emerald500.copy(alpha = 0.25f), Emerald400, "ACCEPTED")
                        entry.severity == LogSeverity.WARNING -> Triple(Amber400.copy(alpha = 0.25f), Amber400, "WARNING")
                        else -> Triple(Slate800, Slate400, "INFO")
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = sevBg
                    ) {
                        Text(
                            text = sevLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = sevText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Timestamp with milliseconds
                Text(
                    text = entry.formattedTime,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Log Title
            Text(
                text = entry.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isMissedOrder) Rose400 else Slate100
            )

            // Log Message
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.message,
                fontSize = 12.sp,
                color = Slate300,
                lineHeight = 16.sp
            )

            // Ride Quick Info Pill (if available)
            if (entry.fare != null || entry.distanceKm != null || entry.pickup != null || entry.drop != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate950.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.fare != null) {
                        Text(
                            text = "₹${entry.fare.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (entry.distanceKm != null) {
                        Text(
                            text = "${entry.distanceKm} km",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Cyan400
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (entry.pickup != null && entry.drop != null) {
                        Text(
                            text = "${entry.pickup} → ${entry.drop}",
                            fontSize = 10.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // MISSED ORDER DIAGNOSTIC CALLOUT (CRITICAL USER TROUBLESHOOTING)
            // -----------------------------------------------------------------
            if (entry.missedReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Rose500.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Rose500.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Rose400,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Why was this order missed?",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Rose400
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.missedReason,
                                    fontSize = 11.sp,
                                    color = Slate200,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (entry.suggestedFix != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "💡 Fix:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber400
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = entry.suggestedFix,
                                    fontSize = 10.sp,
                                    color = Amber400,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Technical Trace / Details
            if (entry.rawDetails != null || entry.packageName != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isExpanded) "Hide Technical Trace" else "Show Technical Trace",
                        fontSize = 10.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate950)
                            .padding(8.dp)
                    ) {
                        if (entry.packageName != null) {
                            Text(
                                text = "Package: ${entry.packageName}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate400
                            )
                        }
                        if (entry.rawDetails != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.rawDetails,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate400,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticTipItem(
    number: String,
    title: String,
    explanation: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Cyan500.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Cyan400
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate200
            )
            Text(
                text = explanation,
                fontSize = 10.sp,
                color = Slate400,
                lineHeight = 14.sp
            )
        }
    }
}

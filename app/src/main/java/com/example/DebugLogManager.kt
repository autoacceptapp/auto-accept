package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ServiceOrigin(val displayName: String) {
    ACCESSIBILITY("Accessibility Service"),
    NOTIFICATION("Ride Notification Service"),
    SYSTEM("System Monitor")
}

enum class LogSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class OrderDebugCategory {
    ORDER_DETECTED,
    ORDER_ACCEPTED,
    ORDER_MISSED,
    FILTER_REJECTED,
    NOTIFICATION_POSTED,
    NOTIFICATION_IGNORED,
    CLICK_EXECUTED,
    CLICK_FAILED,
    SERVICE_STATUS
}

/**
 * Diagnostic log item representing actions and inspection events from
 * both AccessibilityService and RideNotificationService.
 */
data class DebugLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val serviceOrigin: ServiceOrigin,
    val severity: LogSeverity,
    val category: OrderDebugCategory,
    val title: String,
    val message: String,
    val packageName: String? = null,
    val fare: Float? = null,
    val distanceKm: Float? = null,
    val pickup: String? = null,
    val drop: String? = null,
    val missedReason: String? = null,
    val suggestedFix: String? = null,
    val rawDetails: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val isMissedOrder: Boolean
        get() = missedReason != null ||
                category == OrderDebugCategory.ORDER_MISSED ||
                category == OrderDebugCategory.FILTER_REJECTED ||
                category == OrderDebugCategory.CLICK_FAILED
}

/**
 * DebugLogManager maintains an observable circular buffer of the last 50 logs
 * from AccessibilityService and RideNotificationService to help users troubleshoot
 * and diagnose why orders were missed or ignored.
 */
object DebugLogManager {
    private const val TAG = "DebugLogManager"
    const val MAX_LOGS = 50

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(createInitialLogs())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    private fun createInitialLogs(): List<DebugLogEntry> {
        val now = System.currentTimeMillis()
        return listOf(
            DebugLogEntry(
                timestamp = now,
                serviceOrigin = ServiceOrigin.SYSTEM,
                severity = LogSeverity.INFO,
                category = OrderDebugCategory.SERVICE_STATUS,
                title = "Order Debugger Initialized",
                message = "Monitoring Accessibility & Notification services (Buffering last $MAX_LOGS logs)",
                rawDetails = "Diagnostics active for Rapido Captain orders"
            )
        )
    }

    /**
     * Records a new debug event, keeping only the most recent 50 logs.
     */
    fun log(entry: DebugLogEntry) {
        val current = _logs.value
        val updated = (listOf(entry) + current).take(MAX_LOGS)
        _logs.value = updated
        Log.d(TAG, "[${entry.serviceOrigin.name}][${entry.severity.name}] ${entry.title}: ${entry.message}")
    }

    /**
     * Convenience method for Accessibility Service events.
     */
    fun logAccessibility(
        title: String,
        message: String,
        severity: LogSeverity = LogSeverity.INFO,
        category: OrderDebugCategory = OrderDebugCategory.SERVICE_STATUS,
        packageName: String? = null,
        fare: Float? = null,
        distanceKm: Float? = null,
        pickup: String? = null,
        drop: String? = null,
        missedReason: String? = null,
        suggestedFix: String? = null,
        rawDetails: String? = null
    ) {
        log(
            DebugLogEntry(
                serviceOrigin = ServiceOrigin.ACCESSIBILITY,
                severity = severity,
                category = category,
                title = title,
                message = message,
                packageName = packageName,
                fare = fare,
                distanceKm = distanceKm,
                pickup = pickup,
                drop = drop,
                missedReason = missedReason,
                suggestedFix = suggestedFix,
                rawDetails = rawDetails
            )
        )
    }

    /**
     * Convenience method for Ride Notification Service events.
     */
    fun logNotification(
        title: String,
        message: String,
        severity: LogSeverity = LogSeverity.INFO,
        category: OrderDebugCategory = OrderDebugCategory.NOTIFICATION_POSTED,
        packageName: String? = null,
        missedReason: String? = null,
        suggestedFix: String? = null,
        rawDetails: String? = null
    ) {
        log(
            DebugLogEntry(
                serviceOrigin = ServiceOrigin.NOTIFICATION,
                severity = severity,
                category = category,
                title = title,
                message = message,
                packageName = packageName,
                missedReason = missedReason,
                suggestedFix = suggestedFix,
                rawDetails = rawDetails
            )
        )
    }

    /**
     * Clears all debug logs.
     */
    fun clearLogs() {
        _logs.value = listOf(
            DebugLogEntry(
                serviceOrigin = ServiceOrigin.SYSTEM,
                severity = LogSeverity.INFO,
                category = OrderDebugCategory.SERVICE_STATUS,
                title = "Logs Cleared",
                message = "Buffer reset by user. Waiting for new service events."
            )
        )
    }

    /**
     * Simulates various missed order scenarios so drivers can immediately
     * understand how diagnostics work.
     */
    fun simulateSampleMissedOrder(scenarioIndex: Int = -1) {
        val scenarios = listOf(
            // Scenario 1: Distance filter rejected
            {
                logAccessibility(
                    title = "Order Missed: Distance Limit Exceeded",
                    message = "Pickup location is 6.8 km away (configured limit is 5.0 km)",
                    severity = LogSeverity.WARNING,
                    category = OrderDebugCategory.FILTER_REJECTED,
                    packageName = "com.rapido.captain",
                    fare = 145f,
                    distanceKm = 6.8f,
                    pickup = "Outer Ring Road, Bellandur",
                    drop = "Indiranagar 100ft Rd",
                    missedReason = "Pickup distance (6.8 km) exceeds your max distance threshold (5.0 km).",
                    suggestedFix = "Increase the 'Max Distance' slider on Home tab or turn off Distance Filter.",
                    rawDetails = "Evaluated rule: distance (6.8) <= maxDistance (5.0) -> FAIL"
                )
            },
            // Scenario 2: Fare below minimum filter
            {
                logAccessibility(
                    title = "Order Missed: Fare Below Minimum",
                    message = "Incoming ride fare ₹32 is lower than minimum ₹40",
                    severity = LogSeverity.WARNING,
                    category = OrderDebugCategory.FILTER_REJECTED,
                    packageName = "com.rapido.captain",
                    fare = 32f,
                    distanceKm = 1.4f,
                    pickup = "Koramangala 4th Block",
                    drop = "Sony World Signal",
                    missedReason = "Ride fare of ₹32 is less than your configured minimum fare of ₹40.",
                    suggestedFix = "Lower the 'Min Price' filter on Home tab or disable Price Filter.",
                    rawDetails = "Evaluated rule: fare (32.0) >= minPrice (40.0) -> FAIL"
                )
            },
            // Scenario 3: Blacklisted area keyword
            {
                logAccessibility(
                    title = "Order Missed: Blacklisted Keyword",
                    message = "Drop location matched blacklisted keyword 'Airport'",
                    severity = LogSeverity.WARNING,
                    category = OrderDebugCategory.FILTER_REJECTED,
                    packageName = "com.rapido.captain",
                    fare = 520f,
                    distanceKm = 34.0f,
                    pickup = "Hebbal Flyover",
                    drop = "KIA Airport Terminal 1 Gate 3",
                    missedReason = "Drop address matched blacklisted keyword 'Airport'.",
                    suggestedFix = "Remove 'Airport' from your Rejected Keywords in the Blacklist setting if you want airport rides.",
                    rawDetails = "Blacklist filter active: 'Airport, Toll, Slum' matched against 'KIA Airport Terminal 1 Gate 3'"
                )
            },
            // Scenario 4: Notification keyword missing
            {
                logNotification(
                    title = "Notification Ignored (No Ride Keyword)",
                    message = "Notification posted by Rapido but no booking keywords detected",
                    severity = LogSeverity.WARNING,
                    category = OrderDebugCategory.NOTIFICATION_IGNORED,
                    packageName = "com.rapido.captain",
                    missedReason = "Notification title and body did not contain active ride keywords ('new', 'incoming', 'accept', 'ride', 'order').",
                    suggestedFix = "Check if Rapido posted a promotional or wallet reminder instead of a real ride order.",
                    rawDetails = "Title: 'Rapido Captain Daily Bonus' | Body: 'Complete 5 rides to earn extra ₹150 today!'"
                )
            },
            // Scenario 5: Accept button missing or expired
            {
                logAccessibility(
                    title = "Order Missed: Accept Button Not Found",
                    message = "Screen did not contain clickable 'Accept' button after wait delay",
                    severity = LogSeverity.ERROR,
                    category = OrderDebugCategory.CLICK_FAILED,
                    packageName = "com.rapido.captain",
                    fare = 95f,
                    distanceKm = 2.8f,
                    pickup = "HSR Layout Sector 2",
                    drop = "Silk Board Flyover",
                    missedReason = "The accept button was not present in the active window hierarchy. The ride likely timed out or was claimed by another captain.",
                    suggestedFix = "Reduce the 'Accept Delay' in Home tab to 150ms-300ms for faster click response.",
                    rawDetails = "findAcceptButton() returned null. Root node active: com.rapido.captain. Order window expired."
                )
            },
            // Scenario 6: Successful auto-accept for contrast
            {
                logAccessibility(
                    title = "Order Auto-Accepted Successfully",
                    message = "Tapped 'Accept' button in 200ms delay",
                    severity = LogSeverity.SUCCESS,
                    category = OrderDebugCategory.ORDER_ACCEPTED,
                    packageName = "com.rapido.captain",
                    fare = 88f,
                    distanceKm = 2.1f,
                    pickup = "BTM Layout 2nd Stage",
                    drop = "Jayadeva Hospital",
                    rawDetails = "Method: AccessibilityNodeInfo.performAction(ACTION_CLICK) on id/btn_accept"
                )
            }
        )

        val selected = if (scenarioIndex in scenarios.indices) {
            scenarios[scenarioIndex]
        } else {
            scenarios.random()
        }
        selected.invoke()
    }

    /**
     * Formats all current logs as plain text for clipboard copy or sharing.
     */
    fun exportLogsToPlainText(): String {
        val builder = StringBuilder()
        builder.appendLine("=== RAPIDO AUTO ACCEPT DEBUG LOGS (Last ${_logs.value.size} entries) ===")
        builder.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        builder.appendLine("===============================================================\n")

        for ((index, log) in _logs.value.withIndex()) {
            builder.appendLine("[#${index + 1}] ${log.formattedTime} | ${log.serviceOrigin.displayName} | [${log.severity}]")
            builder.appendLine("Title: ${log.title}")
            builder.appendLine("Message: ${log.message}")
            if (log.packageName != null) builder.appendLine("Package: ${log.packageName}")
            if (log.fare != null || log.distanceKm != null) {
                builder.appendLine("Order: Fare=₹${log.fare ?: 0f}, Dist=${log.distanceKm ?: 0f}km")
            }
            if (log.pickup != null || log.drop != null) {
                builder.appendLine("Route: ${log.pickup ?: "?"} -> ${log.drop ?: "?"}")
            }
            if (log.missedReason != null) {
                builder.appendLine("⚠️ MISSED REASON: ${log.missedReason}")
            }
            if (log.suggestedFix != null) {
                builder.appendLine("💡 SUGGESTED FIX: ${log.suggestedFix}")
            }
            if (log.rawDetails != null) {
                builder.appendLine("Technical Details: ${log.rawDetails}")
            }
            builder.appendLine("---------------------------------------------------------------")
        }
        return builder.toString()
    }

    /**
     * Escapes a single CSV column value per RFC 4180 standard.
     */
    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        val str = value.trim()
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    /**
     * Serializes a list of DebugLogEntry items into standard RFC 4180 CSV format with UTF-8 BOM.
     */
    fun exportLogsToCsv(logsToExport: List<DebugLogEntry> = _logs.value): String {
        val sb = StringBuilder()
        // Prepend UTF-8 BOM so Excel and spreadsheet viewers properly decode UTF-8 symbols
        sb.append("\uFEFF")

        val headers = listOf(
            "Log_ID",
            "Timestamp_Millis",
            "Formatted_Time",
            "Service_Origin",
            "Severity",
            "Category",
            "Title",
            "Message",
            "Package_Name",
            "Fare_INR",
            "Distance_KM",
            "Pickup_Location",
            "Drop_Location",
            "Is_Missed_Order",
            "Missed_Reason",
            "Suggested_Fix",
            "Technical_Details"
        )
        sb.appendLine(headers.joinToString(","))

        for (log in logsToExport) {
            val row = listOf(
                escapeCsv(log.id),
                escapeCsv(log.timestamp.toString()),
                escapeCsv(log.formattedTime),
                escapeCsv(log.serviceOrigin.displayName),
                escapeCsv(log.severity.name),
                escapeCsv(log.category.name),
                escapeCsv(log.title),
                escapeCsv(log.message),
                escapeCsv(log.packageName ?: ""),
                escapeCsv(log.fare?.let { "%.2f".format(Locale.US, it) } ?: ""),
                escapeCsv(log.distanceKm?.let { "%.1f".format(Locale.US, it) } ?: ""),
                escapeCsv(log.pickup ?: ""),
                escapeCsv(log.drop ?: ""),
                escapeCsv(if (log.isMissedOrder) "YES" else "NO"),
                escapeCsv(log.missedReason ?: ""),
                escapeCsv(log.suggestedFix ?: ""),
                escapeCsv(log.rawDetails ?: "")
            )
            sb.appendLine(row.joinToString(","))
        }

        return sb.toString()
    }

    /**
     * Exports logs to a CSV file in app cache and opens the Android share sheet
     * to share via email, WhatsApp, Telegram, Google Drive, or messaging apps.
     */
    fun shareLogsAsCsv(
        context: Context,
        logsToShare: List<DebugLogEntry> = _logs.value,
        customSubject: String? = null
    ): Result<File> {
        return runCatching {
            val csvContent = exportLogsToCsv(logsToShare)
            val exportDir = File(context.cacheDir, "debug_exports").apply { mkdirs() }

            // Purge export files older than 24 hours to keep cache clean
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < oneDayAgo) {
                    file.delete()
                }
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "rapido_debug_logs_$timeStamp.csv")
            file.writeText(csvContent, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val dateReadable = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            val subject = customSubject ?: "Rapido Auto Accept Debug Logs ($dateReadable)"
            val summary = "Attached are ${logsToShare.size} diagnostic log entries from Rapido Auto Accept suite.\n" +
                    "Exported on: $dateReadable\n" +
                    "File: ${file.name}"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, summary)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Debug Logs (CSV)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            file
        }
    }
}

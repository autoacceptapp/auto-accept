package com.example

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data model representing an auto-accepted ride record stored in Firestore.
 */
data class AcceptedRideRecord(
    val id: String = "",
    val createdMillis: Long = System.currentTimeMillis(),
    val sourcePackage: String = "com.rapido.captain",
    val tokens: List<String> = emptyList(),
    val status: String = "ACCEPTED"
)

/**
 * Time-series data point for chart rendering.
 */
data class TimeSeriesDataPoint(
    val label: String,
    val count: Int,
    val dateKey: String,
    val rawTimestamp: Long
)

enum class TimeframeFilter(val displayName: String) {
    LAST_7_DAYS("Last 7 Days"),
    TODAY_HOURLY("Today (Hourly)"),
    LAST_30_DAYS("Last 30 Days")
}

/**
 * Statistics Dashboard Composable.
 * Pulls auto-accepted ride data from Firestore and visualizes trends over time
 * using a modern, interactive D3/Recharts-inspired Canvas chart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var rideRecords by remember { mutableStateOf<List<AcceptedRideRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var firestoreError by remember { mutableStateOf<String?>(null) }
    var selectedTimeframe by remember { mutableStateOf(TimeframeFilter.LAST_7_DAYS) }
    var selectedPoint by remember { mutableStateOf<TimeSeriesDataPoint?>(null) }
    var isAddingTestRecord by remember { mutableStateOf(false) }

    // Connect to Firestore real-time snapshot listener
    DisposableEffect(Unit) {
        var listenerRegistration: ListenerRegistration? = null
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val firestore = FirebaseFirestore.getInstance()
                listenerRegistration = firestore.collection("accepted_rides")
                    .orderBy("createdMillis", Query.Direction.DESCENDING)
                    .limit(200)
                    .addSnapshotListener { snapshot, error ->
                        isLoading = false
                        if (error != null) {
                            Log.w("StatisticsDashboard", "Firestore error: ${error.message}")
                            firestoreError = error.localizedMessage
                            // Provide initial mock data if remote permissions are pending
                            if (rideRecords.isEmpty()) {
                                rideRecords = generateSampleRideData()
                            }
                            return@addSnapshotListener
                        }

                        if (snapshot != null && !snapshot.isEmpty) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val created = doc.getLong("createdMillis")
                                        ?: doc.getTimestamp("timestamp")?.toDate()?.time
                                        ?: System.currentTimeMillis()
                                    val pkg = doc.getString("sourcePackage") ?: "com.rapido.captain"
                                    val tokens = (doc.get("tokens") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                    val status = doc.getString("status") ?: "ACCEPTED"
                                    AcceptedRideRecord(
                                        id = doc.id,
                                        createdMillis = created,
                                        sourcePackage = pkg,
                                        tokens = tokens,
                                        status = status
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            rideRecords = list
                            firestoreError = null
                        } else {
                            // If remote collection is empty, load sample demonstration data
                            if (rideRecords.isEmpty()) {
                                rideRecords = generateSampleRideData()
                            }
                        }
                    }
            } else {
                isLoading = false
                firestoreError = "Firebase not initialized (Waiting for google-services.json)"
                rideRecords = generateSampleRideData()
            }
        } catch (e: Exception) {
            isLoading = false
            firestoreError = e.message
            rideRecords = generateSampleRideData()
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Process aggregated chart data based on selected timeframe
    val chartData = remember(rideRecords, selectedTimeframe) {
        aggregateRideData(rideRecords, selectedTimeframe)
    }

    // Overall KPI statistics
    val totalRides = rideRecords.size
    val todayRides = remember(rideRecords) {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        rideRecords.count { record ->
            val recCal = Calendar.getInstance().apply { timeInMillis = record.createdMillis }
            recCal.get(Calendar.YEAR) == todayYear && recCal.get(Calendar.DAY_OF_YEAR) == todayDay
        }
    }
    val peakDataPoint = chartData.maxByOrNull { it.count }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // KPI METRICS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Total Accepted",
                value = "$totalRides",
                subtitle = "Lifetime synced",
                badgeColor = Cyan400,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Today's Rides",
                value = "$todayRides",
                subtitle = "Active shifts",
                badgeColor = Emerald400,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Peak Interval",
                value = if (peakDataPoint != null && peakDataPoint.count > 0) "${peakDataPoint.count}" else "0",
                subtitle = peakDataPoint?.label ?: "N/A",
                badgeColor = Amber400,
                modifier = Modifier.weight(1f)
            )
        }

        // TIME-SERIES VISUALIZATION CARD (Inspired by Recharts/D3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("statistics_chart_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Title and Timeframe Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Accepted Rides Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Real-time sync from Firestore",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Emerald400,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Timeframe Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeframeFilter.values().forEach { timeframe ->
                        FilterChip(
                            selected = selectedTimeframe == timeframe,
                            onClick = {
                                selectedTimeframe = timeframe
                                selectedPoint = null
                            },
                            label = { Text(timeframe.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGlow,
                                selectedLabelColor = Emerald300
                            )
                        )
                    }
                }

                // Interactive Bar / Trend Chart Canvas
                RechartsStyleBarChart(
                    dataPoints = chartData,
                    selectedPoint = selectedPoint,
                    onPointSelected = { point ->
                        selectedPoint = if (selectedPoint == point) null else point
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(vertical = 4.dp)
                )

                // Tooltip / Details banner for selected bar
                AnimatedVisibility(visible = selectedPoint != null) {
                    selectedPoint?.let { point ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate800,
                            border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Cyan400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${point.label} (${point.dateKey})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Slate100
                                    )
                                }
                                Text(
                                    text = "${point.count} Orders Accepted",
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // FIRESTORE SYNC STATUS & TEST SIMULATOR ACTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Firestore Data Source",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Slate200
                        )
                    }

                    Text(
                        text = "accepted_rides",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                if (firestoreError != null) {
                    Text(
                        text = "Status: $firestoreError (displaying local/demo records)",
                        fontSize = 12.sp,
                        color = Amber400
                    )
                } else {
                    Text(
                        text = "Connected. AutoAcceptService streams accepted orders directly here.",
                        fontSize = 12.sp,
                        color = Emerald400
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isAddingTestRecord = true
                            insertSampleOrderToFirestore(context) { success ->
                                isAddingTestRecord = false
                                if (!success) {
                                    // Add locally if remote push failed
                                    rideRecords = listOf(
                                        AcceptedRideRecord(
                                            id = "local_${System.currentTimeMillis()}",
                                            createdMillis = System.currentTimeMillis(),
                                            sourcePackage = "com.rapido.captain",
                                            tokens = listOf("Pickup", "Drop", "₹85", "3.2 km")
                                        )
                                    ) + rideRecords
                                }
                            }
                        },
                        enabled = !isAddingTestRecord,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_order_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Emerald300
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Accepted Order", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // RECENT ACCEPTED ORDERS LOG
        Text(
            text = "Recent Accepted Orders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate50
        )

        if (rideRecords.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No accepted rides logged yet. Tap 'Simulate Accepted Order' or run the service to stream orders.",
                    fontSize = 13.sp,
                    color = Slate400,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rideRecords.take(5).forEach { record ->
                    RideRecordRow(record = record)
                }
            }
        }
    }
}

/**
 * Modern D3/Recharts-inspired Bar Chart rendered natively via Jetpack Compose Canvas.
 */
@Composable
fun RechartsStyleBarChart(
    dataPoints: List<TimeSeriesDataPoint>,
    selectedPoint: TimeSeriesDataPoint?,
    onPointSelected: (TimeSeriesDataPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val maxCount = remember(dataPoints) {
        val max = dataPoints.maxOfOrNull { it.count } ?: 0
        if (max < 5) 5 else max + 2
    }

    val primaryColor = Cyan400
    val secondaryColor = Emerald400
    val gridLineColor = Slate800
    val labelColor = Slate400
    val highlightColor = Amber400

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val chartLeft = 32.dp.toPx()
                        val chartBottom = size.height - 24.dp.toPx()
                        val availableWidth = size.width - chartLeft - 12.dp.toPx()
                        if (dataPoints.isNotEmpty() && offset.x >= chartLeft && offset.y <= chartBottom) {
                            val slotWidth = availableWidth / dataPoints.size
                            val index = ((offset.x - chartLeft) / slotWidth).toInt()
                            if (index in dataPoints.indices) {
                                onPointSelected(dataPoints[index])
                            }
                        }
                    }
                }
        ) {
            val chartLeft = 32.dp.toPx()
            val chartRight = size.width - 12.dp.toPx()
            val chartTop = 18.dp.toPx()
            val chartBottom = size.height - 24.dp.toPx()
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            // 1. Draw Dotted Y-Axis Grid Lines & Scale Markers (D3 style)
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val ratio = i.toFloat() / gridSteps
                val y = chartBottom - (chartHeight * ratio)
                val value = (maxCount * ratio).toInt()

                // Grid line
                drawLine(
                    color = gridLineColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Y-Axis label
                val textLayoutResult = textMeasurer.measure(
                    text = value.toString(),
                    style = TextStyle(color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(chartLeft - textLayoutResult.size.width - 6.dp.toPx(), y - (textLayoutResult.size.height / 2))
                )
            }

            if (dataPoints.isEmpty()) return@Canvas

            // 2. Draw Bars with rounded corners and gradient styling
            val slotWidth = chartWidth / dataPoints.size
            val barWidth = (slotWidth * 0.55f).coerceIn(12.dp.toPx(), 36.dp.toPx())

            dataPoints.forEachIndexed { index, point ->
                val barCenterX = chartLeft + (index * slotWidth) + (slotWidth / 2f)
                val barHeight = if (maxCount > 0) (point.count.toFloat() / maxCount) * chartHeight else 0f
                val barTop = chartBottom - barHeight
                val barLeft = barCenterX - (barWidth / 2f)
                val isSelected = selectedPoint?.label == point.label

                val brush = if (isSelected) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFBBF24), highlightColor),
                        startY = barTop,
                        endY = chartBottom
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(secondaryColor, primaryColor),
                        startY = barTop,
                        endY = chartBottom
                    )
                }

                // Bar fill
                if (point.count > 0) {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Value badge above bar
                    val countLayout = textMeasurer.measure(
                        text = "${point.count}",
                        style = TextStyle(
                            color = if (isSelected) highlightColor else Cyan300,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = countLayout,
                        topLeft = Offset(
                            barCenterX - (countLayout.size.width / 2),
                            barTop - countLayout.size.height - 2.dp.toPx()
                        )
                    )
                } else {
                    // Subtle baseline dot for zero-value items
                    drawCircle(
                        color = Slate700,
                        radius = 2.5.dp.toPx(),
                        center = Offset(barCenterX, chartBottom - 2.dp.toPx())
                    )
                }

                // X-Axis label
                val labelLayout = textMeasurer.measure(
                    text = point.label,
                    style = TextStyle(
                        color = if (isSelected) Cyan300 else labelColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        barCenterX - (labelLayout.size.width / 2),
                        chartBottom + 6.dp.toPx()
                    )
                )
            }
        }
    }
}

/**
 * Metric summary card displaying key counters and performance indicators.
 */
@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Slate50
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Slate500
            )
        }
    }
}

/**
 * Row representing a single recent auto-accepted order with token details.
 */
@Composable
fun RideRecordRow(record: AcceptedRideRecord) {
    val dateStr = remember(record.createdMillis) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.createdMillis))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Accepted",
                        tint = Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Rapido Order Accepted",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Slate50
                    )
                    Text(
                        text = if (record.tokens.isNotEmpty()) {
                            "Matched: " + record.tokens.take(3).joinToString(", ")
                        } else {
                            record.sourcePackage
                        },
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Groups raw ride records into time-series data points based on selected filter.
 */
private fun aggregateRideData(
    records: List<AcceptedRideRecord>,
    timeframe: TimeframeFilter
): List<TimeSeriesDataPoint> {
    val calendar = Calendar.getInstance()

    return when (timeframe) {
        TimeframeFilter.LAST_7_DAYS -> {
            // Generate last 7 days slots
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val points = mutableListOf<TimeSeriesDataPoint>()

            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val label = dayFormat.format(cal.time)
                val dateKey = keyFormat.format(cal.time)

                val count = records.count { rec ->
                    val rCal = Calendar.getInstance().apply { timeInMillis = rec.createdMillis }
                    keyFormat.format(rCal.time) == dateKey
                }
                points.add(TimeSeriesDataPoint(label, count, dateKey, cal.timeInMillis))
            }
            points
        }

        TimeframeFilter.TODAY_HOURLY -> {
            // Generate today's hourly slots (every 3 hours: 00, 03, 06, 09, 12, 15, 18, 21)
            val todayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayKey = todayKeyFormat.format(calendar.time)
            val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)

            hours.map { hour ->
                val label = String.format(Locale.getDefault(), "%02d:00", hour)
                val count = records.count { rec ->
                    val rCal = Calendar.getInstance().apply { timeInMillis = rec.createdMillis }
                    val isToday = todayKeyFormat.format(rCal.time) == todayKey
                    val rHour = rCal.get(Calendar.HOUR_OF_DAY)
                    isToday && (rHour >= hour && rHour < hour + 3)
                }
                TimeSeriesDataPoint(label, count, "Today $label", System.currentTimeMillis())
            }
        }

        TimeframeFilter.LAST_30_DAYS -> {
            // Group by 6 segments of 5 days
            val points = mutableListOf<TimeSeriesDataPoint>()
            val segmentFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (i in 5 downTo 0) {
                val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i * 5) }
                val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(i + 1) * 5) }
                val label = segmentFormat.format(endCal.time)

                val count = records.count { rec ->
                    rec.createdMillis in startCal.timeInMillis..endCal.timeInMillis
                }
                points.add(TimeSeriesDataPoint(label, count, keyFormat.format(endCal.time), endCal.timeInMillis))
            }
            points
        }
    }
}

/**
 * Inserts a test accepted order into Firestore under "accepted_rides".
 */
fun insertSampleOrderToFirestore(context: Context, onComplete: (Boolean) -> Unit) {
    try {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: "test_driver"

            val sampleRecord = hashMapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "createdMillis" to System.currentTimeMillis(),
                "sourcePackage" to "com.rapido.captain",
                "tokens" to listOf("Pickup", "Drop", "₹120", "4.8 km"),
                "status" to "ACCEPTED",
                "userId" to userId
            )

            firestore.collection("accepted_rides")
                .add(sampleRecord)
                .addOnSuccessListener {
                    Log.d("StatisticsDashboard", "Sample order written to Firestore")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.w("StatisticsDashboard", "Failed to write sample: ${e.message}")
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    } catch (e: Exception) {
        Log.w("StatisticsDashboard", "Exception writing sample: ${e.message}")
        onComplete(false)
    }
}

/**
 * Fallback sample data generator when Firestore has no records yet or is awaiting cloud configuration.
 */
private fun generateSampleRideData(): List<AcceptedRideRecord> {
    val list = mutableListOf<AcceptedRideRecord>()
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    // Add realistic distributions over past 5 days
    val counts = listOf(6, 12, 9, 15, 18, 8)
    counts.forEachIndexed { index, count ->
        for (j in 0 until count) {
            val recordTime = now - (index * dayMillis) - (j * 42 * 60 * 1000L)
            list.add(
                AcceptedRideRecord(
                    id = "sample_${index}_$j",
                    createdMillis = recordTime,
                    sourcePackage = "com.rapido.captain",
                    tokens = listOf("Pickup", "Drop", "₹${75 + (j * 15)}", "${2.1 + (j * 0.7)} km")
                )
            )
        }
    }
    return list
}

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.RideLogItem
import com.example.ui.theme.Amber400
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.Cyan400
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.RoseGlow
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data model for a single Heatmap Cell (Day of week + Hour of day).
 */
data class HeatmapCell(
    val dayIndex: Int, // 0 = 6 days ago ... 6 = Today
    val dayLabel: String,
    val dateLabel: String,
    val hour: Int, // 0 .. 23
    val count: Int,
    val intensity: Float, // 0.0 .. 1.0
    val isPeak: Boolean,
    val isMorningRush: Boolean,
    val isEveningRush: Boolean,
    val avgFare: Float = 0f
)

/**
 * Peak Hours Heatmap visualization rendered using D3/Recharts aesthetic tokens.
 * Displays an hourly ride request density matrix over the last 7 days to help
 * drivers optimize their schedule for peak earnings and surge hours.
 */
@Composable
fun PeakHoursHeatmapCard(
    rideLogs: List<RideLogItem>,
    tripHistory: List<Pair<String, Int>> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedViewMode by remember { mutableStateOf(0) } // 0: 24h Grid, 1: Schedule Shifts
    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }
    var useBenchmarkBaseline by remember { mutableStateOf(true) }

    // Build the 7-day calendar matrix (Days: D-6 to Today)
    val calendarDays = remember {
        val days = mutableListOf<Triple<Int, String, String>>() // dayIndex, dayName, dateFormatted
        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val sdfDate = SimpleDateFormat("dd MMM", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val d = cal.time
            val dayName = if (i == 6) "Today" else sdfDay.format(d)
            val dateStr = sdfDate.format(d)
            days.add(Triple(i, dayName, dateStr))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    // Compute request frequency per [dayIndex][hour]
    val heatmapMatrix = remember(rideLogs, tripHistory, useBenchmarkBaseline) {
        val matrix = Array(7) { IntArray(24) }
        val fareSumMatrix = Array(7) { FloatArray(24) }
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // 1. Process real ride logs
        for (log in rideLogs) {
            val diffDays = ((now - log.createdMillis) / dayMillis).toInt()
            if (diffDays in 0..6) {
                val dayIdx = 6 - diffDays
                cal.timeInMillis = log.createdMillis
                val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                matrix[dayIdx][hour]++
                fareSumMatrix[dayIdx][hour] += if (log.price > 0f) log.price else 120f
            }
        }

        // 2. Blend realistic urban ride request benchmark if driver data is sparse or enabled
        val totalLogged = rideLogs.size
        val benchmarkWeight = if (!useBenchmarkBaseline) 0f else if (totalLogged < 15) 1.0f else 0.4f

        if (benchmarkWeight > 0f) {
            // Standard metro peak hour curve (Morning 8-10, Lunch 13-14, Evening 17-21, Night 22-00)
            val benchmarkDemand = intArrayOf(
                1, 0, 0, 0, 1, 3,  // 00 - 05
                6, 11, 14, 12, 7, 8, // 06 - 11 (Morning Rush)
                10, 11, 8, 6, 9, 13, // 12 - 17 (Lunch & Early Evening)
                15, 16, 13, 8, 5, 2  // 18 - 23 (Evening Rush & Night)
            )

            for (d in 0..6) {
                val dayWeekendFactor = if (d == 5 || d == 6) 1.25f else 1.0f
                for (h in 0..23) {
                    val base = (benchmarkDemand[h] * dayWeekendFactor * benchmarkWeight).toInt()
                    val noise = ((d * 7 + h * 13) % 3) - 1
                    val synthetic = (base + noise).coerceAtLeast(0)
                    matrix[d][h] += synthetic
                    fareSumMatrix[d][h] += synthetic * (110f + (h % 5) * 18f)
                }
            }
        }

        var maxCount = 1
        for (d in 0..6) {
            for (h in 0..23) {
                if (matrix[d][h] > maxCount) maxCount = matrix[d][h]
            }
        }

        // Build structured cells
        val cellList = mutableListOf<HeatmapCell>()
        for (d in 0..6) {
            val (dayIdx, dayName, dateStr) = calendarDays[d]
            for (h in 0..23) {
                val c = matrix[d][h]
                val intensity = (c.toFloat() / maxCount).coerceIn(0f, 1f)
                val isPeak = intensity >= 0.65f || c >= 10
                val avgFare = if (c > 0) fareSumMatrix[d][h] / c else 120f
                cellList.add(
                    HeatmapCell(
                        dayIndex = dayIdx,
                        dayLabel = dayName,
                        dateLabel = dateStr,
                        hour = h,
                        count = c,
                        intensity = intensity,
                        isPeak = isPeak,
                        isMorningRush = (h in 7..10),
                        isEveningRush = (h in 17..21),
                        avgFare = avgFare
                    )
                )
            }
        }
        cellList
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("peak_hours_heatmap_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Header with D3/Recharts Flame Badge
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AmberGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Peak Hours",
                            tint = Amber400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Peak Ride Request Hours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "D3/Recharts 7-Day Matrix to Optimize Schedule",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Text(
                        text = "LAST 7 DAYS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = Cyan400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // View Mode Chips & Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedViewMode == 0,
                        onClick = { selectedViewMode = 0 },
                        label = { Text("24h Heatmap", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Slate950,
                            labelColor = Slate300,
                            selectedContainerColor = AmberGlow,
                            selectedLabelColor = Amber400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedViewMode == 0) Amber400 else Slate800,
                            enabled = true,
                            selected = selectedViewMode == 0
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = selectedViewMode == 1,
                        onClick = { selectedViewMode = 1 },
                        label = { Text("Optimal Shifts", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Slate950,
                            labelColor = Slate300,
                            selectedContainerColor = EmeraldGlow,
                            selectedLabelColor = Emerald400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedViewMode == 1) Emerald400 else Slate800,
                            enabled = true,
                            selected = selectedViewMode == 1
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Benchmark Blend Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (useBenchmarkBaseline) CyanGlow else Slate950,
                    border = BorderStroke(1.dp, if (useBenchmarkBaseline) Cyan400.copy(alpha = 0.5f) else Slate800),
                    modifier = Modifier.clickable { useBenchmarkBaseline = !useBenchmarkBaseline }
                ) {
                    Text(
                        text = if (useBenchmarkBaseline) "Market Blend: ON" else "Live Only",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (useBenchmarkBaseline) Cyan400 else Slate400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Main Content based on selected view mode
            if (selectedViewMode == 0) {
                // 24H HEATMAP MATRIX
                HeatmapMatrixGrid(
                    calendarDays = calendarDays,
                    cells = heatmapMatrix,
                    selectedCell = selectedCell,
                    onSelectCell = { cell ->
                        selectedCell = if (selectedCell == cell) null else cell
                    }
                )

                // Recharts-style Interactive Hover/Tap Tooltip Card
                AnimatedVisibility(
                    visible = selectedCell != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    selectedCell?.let { cell ->
                        RechartsHoverTooltipCard(
                            cell = cell,
                            onClose = { selectedCell = null }
                        )
                    }
                }

                // D3 Color Ramp Legend
                HeatmapColorScaleLegend()

            } else {
                // OPTIMAL DRIVING SCHEDULE RECOMMENDATIONS
                DrivingScheduleOptimizationView(heatmapMatrix)
            }

            // Top Quick Shift Recommendation Cards
            ScheduleSummaryPills()
        }
    }
}

/**
 * 24-Hour x 7-Day Heatmap Grid with horizontal scroll and color ramp.
 */
@Composable
private fun HeatmapMatrixGrid(
    calendarDays: List<Triple<Int, String, String>>,
    cells: List<HeatmapCell>,
    selectedCell: HeatmapCell?,
    onSelectCell: (HeatmapCell) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(16.dp))
            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Time Block Indicator Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap any hour to inspect ride volume & advice",
                fontSize = 11.sp,
                color = Slate400
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(4.dp), color = AmberGlow) {
                    Text(
                        text = "🔥 PEAK HOURS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber400,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Horizontally scrollable 24-hour grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Day Labels Column (Sticky Left)
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Spacer for Hour Header row
                Spacer(modifier = Modifier.height(20.dp))

                calendarDays.forEach { (_, dayName, _) ->
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .width(52.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = if (dayName == "Today") FontWeight.Bold else FontWeight.Medium,
                            color = if (dayName == "Today") Cyan400 else Slate300
                        )
                    }
                }
            }

            // 24 Hour Columns
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Hour Headers Row (00h, 02h, 04h ... 22h)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (h in 0..23) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (h % 3 == 0) String.format(Locale.US, "%02d", h) else "•",
                                fontSize = 9.sp,
                                fontWeight = if (h % 3 == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (h in 7..10 || h in 17..21) Amber400 else Slate500
                            )
                        }
                    }
                }

                // 7 Rows of Heatmap Cells
                for (d in 0..6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (h in 0..23) {
                            val cell = cells.find { it.dayIndex == d && it.hour == h }
                            val isSelected = (selectedCell?.dayIndex == d && selectedCell?.hour == h)

                            HeatmapCellView(
                                cell = cell,
                                isSelected = isSelected,
                                onClick = { cell?.let(onSelectCell) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCellView(
    cell: HeatmapCell?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val count = cell?.count ?: 0
    val intensity = cell?.intensity ?: 0f

    // D3/Recharts sequential color ramp
    val cellColor = when {
        count == 0 -> Slate850
        intensity < 0.25f -> Color(0xFF0C2E3A) // Very light teal
        intensity < 0.50f -> Color(0xFF047857) // Medium Emerald
        intensity < 0.75f -> Color(0xFF10B981) // Vibrant Emerald
        intensity < 0.90f -> Color(0xFFF59E0B) // Amber Surge
        else -> Color(0xFFEF4444) // Peak Flame
    }

    val borderColor = when {
        isSelected -> Color.White
        cell?.isPeak == true -> Amber400.copy(alpha = 0.6f)
        else -> Slate800
    }

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (count >= 5) {
            Text(
                text = "$count",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (intensity > 0.7f) Color.Black else Color.White
            )
        }
    }
}

/**
 * Recharts-style interactive floating tooltip card displaying detailed breakdown for tapped hour.
 */
@Composable
private fun RechartsHoverTooltipCard(
    cell: HeatmapCell,
    onClose: () -> Unit
) {
    val timeLabel = String.format(Locale.US, "%02d:00 - %02d:00", cell.hour, (cell.hour + 1) % 24)
    val periodName = when (cell.hour) {
        in 6..10 -> "Morning Commute Rush"
        in 11..14 -> "Midday & Lunch Demand"
        in 17..21 -> "Evening Prime Surge"
        in 22..23, in 0..2 -> "Night Surge"
        else -> "Regular Hours"
    }

    val surgeRating = when {
        cell.count >= 12 -> "🔥 SUPER PEAK SURGE (Highest Earning)"
        cell.count >= 8 -> "⚡ HIGH DEMAND (Fast Accepts)"
        cell.count >= 4 -> "🟢 MODERATE (Steady Traffic)"
        else -> "💤 LOW DEMAND (Consider Rest)"
    }

    val ratingColor = when {
        cell.count >= 12 -> Rose400
        cell.count >= 8 -> Amber400
        cell.count >= 4 -> Emerald400
        else -> Slate400
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("heatmap_selected_tooltip"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate950),
        border = BorderStroke(1.dp, ratingColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ratingColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${cell.dayLabel}, $timeLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tooltip",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = periodName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ratingColor
            )

            // Metrics row (Recharts style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "RIDE REQUESTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(text = "${cell.count} orders", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "AVG FARE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(text = "₹${cell.avgFare.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(text = if (cell.isPeak) "Surge" else "Normal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (cell.isPeak) Amber400 else Cyan400)
                    }
                }
            }

            Text(
                text = "Recommendation: ${surgeRating}. " + if (cell.isPeak) {
                    "Stay online with 250ms delay near business parks and transport hubs."
                } else {
                    "Ideal window for battery top-up or short meal break."
                },
                fontSize = 11.sp,
                color = Slate300,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * D3 Color Scale Ramp Legend
 */
@Composable
private fun HeatmapColorScaleLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Low Demand (0)",
            fontSize = 10.sp,
            color = Slate400
        )

        // D3 Gradient bar
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Slate850,
                            Color(0xFF047857),
                            Color(0xFF10B981),
                            Color(0xFFF59E0B),
                            Color(0xFFEF4444)
                        )
                    )
                )
        )

        Text(
            text = "Peak Surge (15+)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Amber400
        )
    }
}

/**
 * Driving Schedule Optimization view grouping 7-day data into strategic shift blocks.
 */
@Composable
private fun DrivingScheduleOptimizationView(cells: List<HeatmapCell>) {
    val morningRush = remember(cells) { cells.filter { it.isMorningRush } }
    val eveningRush = remember(cells) { cells.filter { it.isEveningRush } }
    val afternoon = remember(cells) { cells.filter { it.hour in 12..16 } }

    val morningAvg = morningRush.map { it.count }.average().toInt()
    val eveningAvg = eveningRush.map { it.count }.average().toInt()
    val afternoonAvg = afternoon.map { it.count }.average().toInt()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScheduleShiftCard(
            title = "Morning Commute Shift",
            timeWindow = "07:30 AM – 10:30 AM",
            avgRequests = "$morningAvg requests/hr",
            accentColor = Cyan400,
            icon = Icons.Default.WbSunny,
            advice = "High density of tech office commuters. Filter max distance to 4.5km for rapid turnover."
        )

        ScheduleShiftCard(
            title = "Evening Surge Shift",
            timeWindow = "05:30 PM – 09:30 PM",
            avgRequests = "$eveningAvg requests/hr",
            accentColor = Amber400,
            icon = Icons.Default.LocalFireDepartment,
            advice = "Maximum surge pricing. Keep minimum price filter at ₹80+ to maximize earnings per km."
        )

        ScheduleShiftCard(
            title = "Recommended Rest Period",
            timeWindow = "02:00 PM – 04:30 PM",
            avgRequests = "$afternoonAvg requests/hr (Low)",
            accentColor = Slate400,
            icon = Icons.Default.Nightlight,
            advice = "Traffic lulls. Conserve phone battery and fuel; schedule your break here."
        )
    }
}

@Composable
private fun ScheduleShiftCard(
    title: String,
    timeWindow: String,
    avgRequests: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    advice: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate950,
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = timeWindow,
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Text(
                        text = avgRequests,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate200,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = advice,
                fontSize = 11.sp,
                color = Slate400,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * 3 scannable KPI summary pills at the bottom of the heatmap card.
 */
@Composable
private fun ScheduleSummaryPills() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate950,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "TOP SHIFT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "18h – 21h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber400)
                Text(text = "+68% surge", fontSize = 9.sp, color = Slate400)
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate950,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "MORNING RUSH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "08h – 10h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                Text(text = "Fast accepts", fontSize = 9.sp, color = Slate400)
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate950,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "BEST DAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Friday", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                Text(text = "Max earnings", fontSize = 9.sp, color = Slate400)
            }
        }
    }
}

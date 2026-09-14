package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CustomFilterRule
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.RoseGlow
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreferencesViewModel = viewModel()
) {
    val filterSettings by viewModel.filterSettings.collectAsStateWithLifecycle()
    val customRules by viewModel.customRules.collectAsStateWithLifecycle()

    var showAddRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CustomFilterRule?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.testTag("preferences_screen"),
        containerColor = Slate950,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ride Preferences",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("preferences_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate200
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.setMinFare(40f, true)
                            viewModel.setMaxDistance(5.0f, true)
                            viewModel.setPassengerRating(4.5f, false)
                        },
                        modifier = Modifier.testTag("preferences_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Defaults",
                            tint = Slate400
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Slate100
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. OVERVIEW & ACTIVE STATUS BANNER
            PreferencesOverviewCard(
                filterSettings = filterSettings,
                activeCustomRulesCount = customRules.count { it.isActive }
            )

            // 2. CORE FILTERING RULES (MIN FARE, DISTANCE, PASSENGER RATING)
            Text(
                text = "CORE FILTERING THRESHOLDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Minimum Fare Preference Card
            MinimumFarePreferenceCard(
                isEnabled = filterSettings.isPriceFilterOn,
                minFare = filterSettings.minPrice,
                onToggle = { enabled -> viewModel.setMinFare(filterSettings.minPrice, enabled) },
                onFareChanged = { fare -> viewModel.setMinFare(fare, filterSettings.isPriceFilterOn) }
            )

            // Maximum Pickup Distance Preference Card
            MaximumDistancePreferenceCard(
                isEnabled = filterSettings.isDistanceFilterOn,
                maxDistanceKm = filterSettings.maxDistance,
                onToggle = { enabled -> viewModel.setMaxDistance(filterSettings.maxDistance, enabled) },
                onDistanceChanged = { dist -> viewModel.setMaxDistance(dist, filterSettings.isDistanceFilterOn) }
            )

            // Passenger Rating Preference Card
            PassengerRatingPreferenceCard(
                isEnabled = filterSettings.isPassengerRatingFilterOn,
                minRating = filterSettings.minPassengerRating,
                onToggle = { enabled -> viewModel.setPassengerRating(filterSettings.minPassengerRating, enabled) },
                onRatingChanged = { rating -> viewModel.setPassengerRating(rating, filterSettings.isPassengerRatingFilterOn) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. CUSTOM FILTERING RULES (ROOM PERSISTENCE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CUSTOM FILTERING RULES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        text = "Stored locally in Room database",
                        fontSize = 11.sp,
                        color = Cyan400,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Button(
                    onClick = { showAddRuleDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_rule_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "New Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Rule Presets Scroll Row
            Text(
                text = "Quick Presets:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPresetChip(
                    label = "✈️ Airport Express",
                    testTag = "preset_airport_rule",
                    onClick = { viewModel.addPresetRule("airport") }
                )
                QuickPresetChip(
                    label = "💰 High Fare Trips",
                    testTag = "preset_high_fare_rule",
                    onClick = { viewModel.addPresetRule("high_fare") }
                )
                QuickPresetChip(
                    label = "⭐ 5-Star VIPs",
                    testTag = "preset_five_star_rule",
                    onClick = { viewModel.addPresetRule("five_star") }
                )
                QuickPresetChip(
                    label = "⚡ Quick Local Hops",
                    testTag = "preset_short_hops_rule",
                    onClick = { viewModel.addPresetRule("short_hops") }
                )
            }

            // List of Custom Filter Rules
            if (customRules.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Rule,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No custom rules yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create rules above or tap a preset to auto-populate from Room.",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            } else {
                customRules.forEach { rule ->
                    CustomRuleItemCard(
                        rule = rule,
                        onToggle = { active -> viewModel.toggleCustomRule(rule.id, active) },
                        onEdit = { ruleToEdit = rule },
                        onDelete = { showDeleteConfirmDialog = rule.id },
                        modifier = Modifier.testTag("custom_rule_card_${rule.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. INTERACTIVE RIDE SIMULATOR / TEST SANDBOX
            RideFilterSimulatorCard(
                viewModel = viewModel,
                modifier = Modifier.testTag("preferences_simulator_card")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add / Edit Custom Rule Dialog
    if (showAddRuleDialog || ruleToEdit != null) {
        val existingRule = ruleToEdit
        RuleEditorDialog(
            initialRule = existingRule,
            onDismiss = {
                showAddRuleDialog = false
                ruleToEdit = null
            },
            onSave = { name, minFare, maxFare, maxDist, minRating, isMinFare, isMaxDist, isRating, keyword ->
                if (existingRule != null) {
                    viewModel.updateCustomRule(
                        existingRule.copy(
                            name = name,
                            minFare = minFare,
                            maxFare = maxFare,
                            maxDistanceKm = maxDist,
                            minPassengerRating = minRating,
                            isMinFareEnabled = isMinFare,
                            isMaxDistanceEnabled = isMaxDist,
                            isRatingFilterEnabled = isRating,
                            destinationKeyword = keyword
                        )
                    )
                } else {
                    viewModel.addCustomRule(
                        name = name,
                        minFare = minFare,
                        maxFare = maxFare,
                        maxDistanceKm = maxDist,
                        minRating = minRating,
                        isMinFareEnabled = isMinFare,
                        isMaxDistanceEnabled = isMaxDist,
                        isRatingEnabled = isRating,
                        destinationKeyword = keyword
                    )
                }
                showAddRuleDialog = false
                ruleToEdit = null
            }
        )
    }

    // Confirm Delete Dialog
    showDeleteConfirmDialog?.let { ruleId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = Slate900,
            title = { Text("Delete Filtering Rule?", color = Slate100, fontWeight = FontWeight.Bold) },
            text = { Text("This rule will be removed from your Room database.", color = Slate300, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomRule(ruleId)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

// =========================================================================
// OVERVIEW STATUS CARD
// =========================================================================
@Composable
fun PreferencesOverviewCard(
    filterSettings: com.example.data.FilterSettings,
    activeCustomRulesCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preferences_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Cyan500.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Filtering Rules",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = "Backed by Room Database",
                            fontSize = 11.sp,
                            color = Cyan400
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGlow,
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "ROOM ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Slate800)
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Status Pill Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterMetricPill(
                    icon = Icons.Default.Payments,
                    label = "Min Fare",
                    value = if (filterSettings.isPriceFilterOn) "₹${filterSettings.minPrice.toInt()}" else "OFF",
                    isActive = filterSettings.isPriceFilterOn,
                    activeColor = Emerald400
                )
                FilterMetricPill(
                    icon = Icons.Default.Navigation,
                    label = "Max Distance",
                    value = if (filterSettings.isDistanceFilterOn) "${filterSettings.maxDistance} km" else "OFF",
                    isActive = filterSettings.isDistanceFilterOn,
                    activeColor = Cyan400
                )
                FilterMetricPill(
                    icon = Icons.Default.Star,
                    label = "Min Rating",
                    value = if (filterSettings.isPassengerRatingFilterOn) "${filterSettings.minPassengerRating}★" else "OFF",
                    isActive = filterSettings.isPassengerRatingFilterOn,
                    activeColor = Amber400
                )
                FilterMetricPill(
                    icon = Icons.Default.Rule,
                    label = "Custom Rules",
                    value = "$activeCustomRulesCount Active",
                    isActive = activeCustomRulesCount > 0,
                    activeColor = Violet500
                )
            }
        }
    }
}

@Composable
fun FilterMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    isActive: Boolean,
    activeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else Slate500,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Slate100 else Slate500
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Slate400
        )
    }
}

// =========================================================================
// 1. MINIMUM FARE PREFERENCE CARD
// =========================================================================
@Composable
fun MinimumFarePreferenceCard(
    isEnabled: Boolean,
    minFare: Float,
    onToggle: (Boolean) -> Unit,
    onFareChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetFares = listOf(30f, 50f, 80f, 100f, 150f, 200f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pref_fare_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (isEnabled) Emerald500.copy(alpha = 0.5f) else Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEnabled) EmeraldGlow else Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (isEnabled) Emerald400 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Minimum Fare Filter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = if (isEnabled) "Auto-skip rides below ₹${minFare.toInt()}" else "Filter Disabled (Any fare)",
                            fontSize = 12.sp,
                            color = if (isEnabled) Emerald400 else Slate400
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Emerald500,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("pref_min_fare_switch")
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Threshold: ₹${minFare.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text(text = "Target Fare", fontSize = 11.sp, color = Slate400)
                    }

                    Slider(
                        value = minFare,
                        onValueChange = { onFareChanged(it) },
                        valueRange = 20f..400f,
                        steps = 37,
                        colors = SliderDefaults.colors(
                            thumbColor = Emerald400,
                            activeTrackColor = Emerald500,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("pref_min_fare_slider")
                    )

                    // Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetFares.forEach { preset ->
                            val isSelected = minFare.toInt() == preset.toInt()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Emerald500 else Slate850,
                                border = BorderStroke(1.dp, if (isSelected) Emerald400 else Slate700),
                                modifier = Modifier
                                    .clickable { onFareChanged(preset) }
                                    .testTag("chip_fare_${preset.toInt()}")
                            ) {
                                Text(
                                    text = "₹${preset.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Slate300,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. MAXIMUM DISTANCE PREFERENCE CARD
// =========================================================================
@Composable
fun MaximumDistancePreferenceCard(
    isEnabled: Boolean,
    maxDistanceKm: Float,
    onToggle: (Boolean) -> Unit,
    onDistanceChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetDistances = listOf(1.5f, 2.5f, 3.5f, 5.0f, 8.0f, 12.0f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pref_distance_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (isEnabled) Cyan500.copy(alpha = 0.5f) else Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEnabled) CyanGlow else Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (isEnabled) Cyan400 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Maximum Pickup Distance",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = if (isEnabled) "Ignore pickups further than $maxDistanceKm km" else "Filter Disabled (Any distance)",
                            fontSize = 12.sp,
                            color = if (isEnabled) Cyan400 else Slate400
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Cyan500,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("pref_distance_switch")
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Max Limit: $maxDistanceKm km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text(text = "Pickup Radius", fontSize = 11.sp, color = Slate400)
                    }

                    Slider(
                        value = maxDistanceKm,
                        onValueChange = { onDistanceChanged(Math.round(it * 10) / 10f) },
                        valueRange = 1.0f..20.0f,
                        steps = 37,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan400,
                            activeTrackColor = Cyan500,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("pref_distance_slider")
                    )

                    // Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetDistances.forEach { preset ->
                            val isSelected = maxDistanceKm == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Cyan500 else Slate850,
                                border = BorderStroke(1.dp, if (isSelected) Cyan400 else Slate700),
                                modifier = Modifier
                                    .clickable { onDistanceChanged(preset) }
                                    .testTag("chip_dist_${preset.toString().replace('.', '_')}")
                            ) {
                                Text(
                                    text = "$preset km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Slate300,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. PASSENGER RATING PREFERENCE CARD
// =========================================================================
@Composable
fun PassengerRatingPreferenceCard(
    isEnabled: Boolean,
    minRating: Float,
    onToggle: (Boolean) -> Unit,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetRatings = listOf(4.0f, 4.2f, 4.5f, 4.7f, 4.8f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pref_rating_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (isEnabled) Amber500.copy(alpha = 0.5f) else Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEnabled) AmberGlow else Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isEnabled) Amber400 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Passenger Rating Filter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = if (isEnabled) "Accept only riders with rating ≥ ${minRating}★" else "Filter Disabled (All ratings)",
                            fontSize = 12.sp,
                            color = if (isEnabled) Amber400 else Slate400
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Amber500,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("pref_rating_switch")
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Minimum Rating: $minRating", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate100)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Amber400, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = when {
                                minRating >= 4.7f -> "Top Tier VIPs"
                                minRating >= 4.4f -> "High Quality"
                                else -> "Standard Riders"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber400
                        )
                    }

                    Slider(
                        value = minRating,
                        onValueChange = { onRatingChanged(Math.round(it * 10) / 10f) },
                        valueRange = 3.5f..4.9f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = Amber400,
                            activeTrackColor = Amber500,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("pref_rating_slider")
                    )

                    // Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetRatings.forEach { preset ->
                            val isSelected = minRating == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Amber500 else Slate850,
                                border = BorderStroke(1.dp, if (isSelected) Amber400 else Slate700),
                                modifier = Modifier
                                    .clickable { onRatingChanged(preset) }
                                    .testTag("chip_rating_${preset.toString().replace('.', '_')}")
                            ) {
                                Text(
                                    text = "$preset★",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Slate950 else Slate300,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// CUSTOM FILTER RULE ITEM CARD
// =========================================================================
@Composable
fun CustomRuleItemCard(
    rule: CustomFilterRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (rule.isActive) Violet500.copy(alpha = 0.4f) else Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (rule.isActive) Violet500.copy(alpha = 0.2f) else Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Rule,
                            contentDescription = null,
                            tint = if (rule.isActive) Violet500 else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = rule.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = if (rule.isActive) "ACTIVE IN ROOM" else "PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (rule.isActive) Emerald400 else Slate500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_custom_rule_${rule.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_custom_rule_${rule.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Rose400, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Violet500,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("toggle_custom_rule_${rule.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rule Criteria Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (rule.isMinFareEnabled) {
                    RuleBadge(label = "Min: ₹${rule.minFare.toInt()}", color = Emerald400, bgColor = EmeraldGlow)
                }
                if (rule.isMaxDistanceEnabled) {
                    RuleBadge(label = "Max: ${rule.maxDistanceKm} km", color = Cyan400, bgColor = CyanGlow)
                }
                if (rule.isRatingFilterEnabled) {
                    RuleBadge(label = "Rating ≥ ${rule.minPassengerRating}★", color = Amber400, bgColor = AmberGlow)
                }
                if (rule.destinationKeyword.isNotBlank()) {
                    RuleBadge(label = "Loc: \"${rule.destinationKeyword}\"", color = Violet500, bgColor = Violet500.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
fun RuleBadge(label: String, color: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun QuickPresetChip(label: String, testTag: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate700),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Slate200,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// =========================================================================
// 4. INTERACTIVE RIDE SIMULATOR / TEST SANDBOX
// =========================================================================
@Composable
fun RideFilterSimulatorCard(
    viewModel: PreferencesViewModel,
    modifier: Modifier = Modifier
) {
    var testFare by remember { mutableFloatStateOf(85f) }
    var testDistance by remember { mutableFloatStateOf(3.2f) }
    var testRating by remember { mutableFloatStateOf(4.6f) }
    var testDestination by remember { mutableStateOf("City Airport Terminal 1") }

    val simulationResult = remember(testFare, testDistance, testRating, testDestination, viewModel.filterSettings, viewModel.customRules) {
        viewModel.simulateRide(testFare, testDistance, testRating, testDestination)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Filter Simulator & Test Sandbox",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = "Test how incoming rides evaluate against your Room rules",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Fare: ₹${testFare.toInt()}", fontSize = 11.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = testFare,
                        onValueChange = { testFare = it },
                        valueRange = 20f..250f,
                        colors = SliderDefaults.colors(thumbColor = Emerald400, activeTrackColor = Emerald500),
                        modifier = Modifier.testTag("sim_fare_slider")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Dist: ${Math.round(testDistance * 10) / 10f}km", fontSize = 11.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = testDistance,
                        onValueChange = { testDistance = Math.round(it * 10) / 10f },
                        valueRange = 0.5f..15.0f,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan500),
                        modifier = Modifier.testTag("sim_dist_slider")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Test Rating: ${Math.round(testRating * 10) / 10f}★", fontSize = 11.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = testRating,
                        onValueChange = { testRating = Math.round(it * 10) / 10f },
                        valueRange = 3.5f..5.0f,
                        colors = SliderDefaults.colors(thumbColor = Amber400, activeTrackColor = Amber500),
                        modifier = Modifier.testTag("sim_rating_slider")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = testDestination,
                        onValueChange = { testDestination = it },
                        label = { Text("Destination", fontSize = 10.sp) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Slate100, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700
                        ),
                        singleLine = true,
                        modifier = Modifier.testTag("sim_destination_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulation Verdict Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (simulationResult.isAccepted) EmeraldGlow else RoseGlow,
                border = BorderStroke(1.dp, if (simulationResult.isAccepted) Emerald500 else Rose500),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulation_result_badge")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (simulationResult.isAccepted) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (simulationResult.isAccepted) Emerald400 else Rose400,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (simulationResult.isAccepted) "WOULD AUTO-ACCEPT" else "WOULD REJECT / IGNORE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (simulationResult.isAccepted) Emerald400 else Rose400
                        )
                        Text(
                            text = simulationResult.reasons.joinToString(" • "),
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// RULE EDITOR DIALOG (ADD / EDIT)
// =========================================================================
@Composable
fun RuleEditorDialog(
    initialRule: CustomFilterRule?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        minFare: Float,
        maxFare: Float,
        maxDistanceKm: Float,
        minRating: Float,
        isMinFareEnabled: Boolean,
        isMaxDistanceEnabled: Boolean,
        isRatingEnabled: Boolean,
        destinationKeyword: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var minFare by remember { mutableFloatStateOf(initialRule?.minFare ?: 60f) }
    var maxDistance by remember { mutableFloatStateOf(initialRule?.maxDistanceKm ?: 5.0f) }
    var minRating by remember { mutableFloatStateOf(initialRule?.minPassengerRating ?: 4.5f) }
    var destinationKeyword by remember { mutableStateOf(initialRule?.destinationKeyword ?: "") }

    var isMinFareEnabled by remember { mutableStateOf(initialRule?.isMinFareEnabled ?: true) }
    var isMaxDistanceEnabled by remember { mutableStateOf(initialRule?.isMaxDistanceEnabled ?: true) }
    var isRatingEnabled by remember { mutableStateOf(initialRule?.isRatingFilterEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text(
                text = if (initialRule != null) "Edit Custom Rule" else "New Filtering Rule",
                color = Slate100,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name (e.g. Airport Only)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_name_input")
                )

                // Min Fare Checkbox & Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min Fare: ₹${minFare.toInt()}", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isMinFareEnabled,
                            onCheckedChange = { isMinFareEnabled = it },
                            modifier = Modifier.testTag("rule_min_fare_switch")
                        )
                    }
                    if (isMinFareEnabled) {
                        Slider(
                            value = minFare,
                            onValueChange = { minFare = it },
                            valueRange = 20f..400f,
                            colors = SliderDefaults.colors(thumbColor = Emerald400, activeTrackColor = Emerald500),
                            modifier = Modifier.testTag("rule_min_fare_slider")
                        )
                    }
                }

                // Max Distance Checkbox & Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max Distance: ${maxDistance}km", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isMaxDistanceEnabled,
                            onCheckedChange = { isMaxDistanceEnabled = it },
                            modifier = Modifier.testTag("rule_max_dist_switch")
                        )
                    }
                    if (isMaxDistanceEnabled) {
                        Slider(
                            value = maxDistance,
                            onValueChange = { maxDistance = Math.round(it * 10) / 10f },
                            valueRange = 1.0f..20.0f,
                            colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan500),
                            modifier = Modifier.testTag("rule_max_dist_slider")
                        )
                    }
                }

                // Passenger Rating Checkbox & Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min Rating: ${minRating}★", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isRatingEnabled,
                            onCheckedChange = { isRatingEnabled = it },
                            modifier = Modifier.testTag("rule_min_rating_switch")
                        )
                    }
                    if (isRatingEnabled) {
                        Slider(
                            value = minRating,
                            onValueChange = { minRating = Math.round(it * 10) / 10f },
                            valueRange = 3.5f..4.9f,
                            colors = SliderDefaults.colors(thumbColor = Amber400, activeTrackColor = Amber500),
                            modifier = Modifier.testTag("rule_min_rating_slider")
                        )
                    }
                }

                OutlinedTextField(
                    value = destinationKeyword,
                    onValueChange = { destinationKeyword = it },
                    label = { Text("Required Location Keyword (Optional)", color = Slate400) },
                    placeholder = { Text("e.g. Airport, Terminal, Metro", color = Slate500) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_destination_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { "Custom Filter" },
                        minFare,
                        5000f,
                        maxDistance,
                        minRating,
                        isMinFareEnabled,
                        isMaxDistanceEnabled,
                        isRatingEnabled,
                        destinationKeyword
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                modifier = Modifier.testTag("save_custom_rule_button")
            ) {
                Text("Save to Room", fontWeight = FontWeight.Bold, color = Slate950)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}

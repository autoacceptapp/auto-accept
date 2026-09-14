package com.example.ui

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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AccessibilityFaqItem
import com.example.RemoteConfigManager
import com.example.ui.theme.Cyan400
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

/**
 * Searchable FAQ section in the Settings tab that pulls content dynamically
 * from Firebase Remote Config, allowing users to find troubleshooting steps
 * for common accessibility issues.
 */
@Composable
fun AccessibilityFaqSection(
    onOpenAccessibility: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotificationListener: () -> Unit,
    modifier: Modifier = Modifier,
    initialSearchQuery: String = ""
) {
    val context = LocalContext.current
    val faqs by RemoteConfigManager.accessibilityFaqs.collectAsState()
    val fetchStatus by RemoteConfigManager.lastFetchStatus.collectAsState()

    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isRefreshing by remember { mutableStateOf(false) }
    val expandedFaqIds = remember { mutableStateOf(setOf<String>()) }

    val categories = remember(faqs) {
        val cats = mutableListOf("All")
        cats.addAll(faqs.map { it.category }.distinct())
        cats
    }

    val filteredFaqs = remember(faqs, searchQuery, selectedCategory) {
        faqs.filter { item ->
            val matchesCategory = (selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true))
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.question.lowercase().contains(q) ||
                    item.answer.lowercase().contains(q) ||
                    item.category.lowercase().contains(q) ||
                    item.stepGuide.any { it.lowercase().contains(q) }
            }
            matchesCategory && matchesSearch
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("accessibility_faq_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Remote Config Cloud Badge & Refresh
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
                            .background(CyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Accessibility FAQ",
                            tint = Cyan400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Accessibility Troubleshooting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Firebase Remote Config: $fetchStatus",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = {
                        if (!isRefreshing) {
                            isRefreshing = true
                            RemoteConfigManager.fetchLatestFaqs(context) { success ->
                                isRefreshing = false
                                Toast.makeText(
                                    context,
                                    if (success) "FAQs updated from Remote Config" else "Using cached Remote Config FAQs",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("faq_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Cyan400
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync from Remote Config",
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = "Instant step-by-step guides for common Android accessibility permissions, background kill issues, and detection glitches.",
                fontSize = 12.sp,
                color = Slate300,
                lineHeight = 16.sp
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search troubleshooting steps (e.g. battery, restricted, overlay)...",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search FAQs",
                        tint = Cyan400,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("faq_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950,
                    focusedBorderColor = Cyan400,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Slate50,
                    unfocusedTextColor = Slate200
                ),
                singleLine = true
            )

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategory == cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Slate950,
                            labelColor = Slate300,
                            selectedContainerColor = CyanGlow,
                            selectedLabelColor = Cyan400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Cyan400 else Slate800,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // FAQ Items List
            if (filteredFaqs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No troubleshooting articles found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        Text(
                            text = "Try searching for 'battery', 'settings', or clear your filter.",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.5f))
                        ) {
                            Text("Reset Search Filters", fontSize = 11.sp, color = Cyan400)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredFaqs.forEach { faq ->
                        val isExpanded = expandedFaqIds.value.contains(faq.id)
                        FaqAccordionItem(
                            item = faq,
                            isExpanded = isExpanded,
                            onToggle = {
                                val current = expandedFaqIds.value.toMutableSet()
                                if (isExpanded) current.remove(faq.id) else current.add(faq.id)
                                expandedFaqIds.value = current
                            },
                            onActionClick = { action ->
                                when (action) {
                                    "OPEN_ACCESSIBILITY" -> onOpenAccessibility()
                                    "OPEN_BATTERY" -> onOpenBatteryOptimization()
                                    "OPEN_OVERLAY" -> onOpenOverlay()
                                    "OPEN_NOTIFICATIONS" -> onOpenNotificationListener()
                                    else -> onOpenAccessibility()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionItem(
    item: AccessibilityFaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onActionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val categoryColor = when (item.category.lowercase()) {
        "permissions" -> Rose400
        "battery & os", "battery" -> Emerald400
        "detection" -> Cyan400
        "overlay" -> Color(0xFFA78BFA)
        else -> Cyan400
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("faq_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Slate850 else Slate950
        ),
        border = BorderStroke(
            1.dp,
            if (isExpanded) categoryColor.copy(alpha = 0.4f) else Slate800
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFaqIcon(item.category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, categoryColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Answer text
                    Text(
                        text = item.answer,
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )

                    // Step Guide Checklist
                    if (item.stepGuide.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Step-by-Step Fix:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                                item.stepGuide.forEachIndexed { index, step ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldGlow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald400
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = step,
                                            fontSize = 11.sp,
                                            color = Slate200,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Direct action button if applicable
                        if (!item.directAction.isNullOrBlank()) {
                            val actionLabel = when (item.directAction) {
                                "OPEN_ACCESSIBILITY" -> "Open Accessibility"
                                "OPEN_BATTERY" -> "Disable Battery Saver"
                                "OPEN_OVERLAY" -> "Open Overlay Settings"
                                "OPEN_NOTIFICATIONS" -> "Open Notifications"
                                else -> "Open Settings"
                            }
                            Button(
                                onClick = { onActionClick(item.directAction) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = categoryColor,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = actionLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Copy Steps Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val fullText = buildString {
                                    appendLine(item.question)
                                    appendLine(item.answer)
                                    if (item.stepGuide.isNotEmpty()) {
                                        appendLine("\nTroubleshooting Steps:")
                                        item.stepGuide.forEachIndexed { idx, st ->
                                            appendLine("${idx + 1}. $st")
                                        }
                                    }
                                }
                                clipboard.setPrimaryClip(ClipData.newPlainText("RideStrike FAQ", fullText))
                                Toast.makeText(context, "Copied steps to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate800),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Steps",
                                tint = Slate300,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                fontSize = 11.sp,
                                color = Slate300
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFaqIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "permissions" -> Icons.Default.Security
        "battery & os", "battery" -> Icons.Default.BatteryAlert
        "detection" -> Icons.Default.Speed
        "overlay" -> Icons.Default.Layers
        "notifications" -> Icons.Default.Notifications
        else -> Icons.Default.HelpOutline
    }
}

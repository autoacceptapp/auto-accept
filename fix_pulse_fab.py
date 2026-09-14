import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.material3.FloatingActionButton
"""
content = re.sub(
    r'(import androidx\.compose\.runtime\.remember\n)',
    r'\1' + imports,
    content
)

# 1. Add FloatingActionButton to Scaffold in AutoAcceptDashboardScreen
fab_code = """                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isMasterSwitchOn = !isMasterSwitchOn
                    AutoAcceptService.setAutomationEnabled(context, isMasterSwitchOn)
                },
                containerColor = if (isMasterSwitchOn && isAccessibilityEnabled) Emerald500 else Slate700,
                contentColor = Slate950,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("dashboard_fab_toggle")
            ) {
                Icon(
                    imageVector = if (isMasterSwitchOn && isAccessibilityEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Service"
                )
            }
        }
    ) { innerPadding ->"""
content = re.sub(
    r'                    modifier = Modifier\.testTag\("nav_settings"\)\n                \)\n            \}\n        \}\n    \) \{ innerPadding ->',
    fab_code,
    content
)

# 2. Add Pulse Animation to VisualLogViewCard
pulse_code = """    val acceptedCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.ORDER_ACCEPTED }
    }

    val ignoredCount = remember(serviceEvents) {
        serviceEvents.count { it.type == ServiceEventType.ORDER_IGNORED }
    }

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(acceptedCount) {
        if (acceptedCount > 0) {
            pulseScale.animateTo(
                targetValue = 1.02f,
                animationSpec = tween(150)
            )
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(300)
            )
        }
    }

    val displayedEvents = if (isExpanded) filteredEvents else filteredEvents.take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale.value)
            .testTag("visual_log_card"),"""
            
content = re.sub(
    r'    val acceptedCount = remember\(serviceEvents\) \{\n        serviceEvents\.count \{ it\.type == ServiceEventType\.ORDER_ACCEPTED \}\n    \}\n\n    val ignoredCount = remember\(serviceEvents\) \{\n        serviceEvents\.count \{ it\.type == ServiceEventType\.ORDER_IGNORED \}\n    \}\n\n    val displayedEvents = if \(isExpanded\) filteredEvents else filteredEvents\.take\(5\)\n\n    Card\(\n        modifier = Modifier\n            \.fillMaxWidth\(\)\n            \.testTag\("visual_log_card"\),',
    pulse_code,
    content
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)


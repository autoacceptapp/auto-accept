import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

ride_data_classes = """data class RideData(
    val price: Float?,
    val distanceKm: Float?,
    val passengerRating: Float?,
    val pickup: String,
    val drop: String,
    val sourcePackage: String,
    val signature: String,
    val isPremium: Boolean,
    val delayMs: Long
)

data class FilterDecision(
    val passed: Boolean,
    val reason: String,
    val failedFilter: String? = null
)
"""
if "data class RideData(" not in content:
    content = content.replace("class AutoAcceptService : AccessibilityService(), TextToSpeech.OnInitListener {", ride_data_classes + "\nclass AutoAcceptService : AccessibilityService(), TextToSpeech.OnInitListener {")

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

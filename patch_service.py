import re

with open("app/src/main/java/com/example/AutoAcceptService.kt", "r") as f:
    content = f.read()

# 1. Update RideData
ride_data_old = """data class RideData(
    val price: Float?,
    val distanceKm: Float?,
    val passengerRating: Float?,
    val pickup: String,
    val drop: String,
    val sourcePackage: String,
    val signature: String,
    val isPremium: Boolean,
    val delayMs: Long
)"""
ride_data_new = """data class RideData(
    val price: Float?,
    val distanceKm: Float?,
    val passengerRating: Float?,
    val pickup: String?,
    val drop: String?,
    val sourcePackage: String,
    val signature: String,
    val isPremium: Boolean,
    val delayMs: Long
)"""
content = content.replace(ride_data_old, ride_data_new)

# 2. Update generateRideSignature
sig_old = """        fun generateRideSignature(sourcePackage: String, price: Float?, distanceKm: Float?, pickup: String, drop: String): String {
            val p = price?.toInt()?.toString() ?: "any_fare"
            val d = distanceKm?.let { String.format(Locale.US, "%.1f", it) } ?: "any_dist"
            val pick = pickup.trim().lowercase().take(30)
            val drp = drop.trim().lowercase().take(30)
            return "$sourcePackage#$p#$d#$pick#$drp"
        }"""
sig_new = """        fun generateRideSignature(sourcePackage: String, price: Float?, distanceKm: Float?, rating: Float?, pickup: String?, drop: String?): String {
            val p = price?.let { String.format(Locale.US, "%.1f", it) } ?: "null_fare"
            val d = distanceKm?.let { String.format(Locale.US, "%.1f", it) } ?: "null_dist"
            val r = rating?.let { String.format(Locale.US, "%.1f", it) } ?: "null_rating"
            val pick = pickup?.trim()?.lowercase()?.take(30) ?: "null_pickup"
            val drp = drop?.trim()?.lowercase()?.take(30) ?: "null_drop"
            return "$sourcePackage#$p#$d#$r#$pick#$drp"
        }"""
content = content.replace(sig_old, sig_new)

with open("app/src/main/java/com/example/AutoAcceptService.kt", "w") as f:
    f.write(content)

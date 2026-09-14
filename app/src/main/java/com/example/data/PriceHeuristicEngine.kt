package com.example.data

data class PriceTierStats(
    val tierName: String,
    val rangeLabel: String,
    val totalCount: Int,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val acceptRatePercent: Int
)

data class PriceRecommendation(
    val suggestedMinPrice: Float,
    val suggestedMaxPrice: Float,
    val rationale: String,
    val confidenceBadge: String,
    val totalDecisions: Int,
    val acceptedDecisions: Int,
    val rejectedDecisions: Int,
    val lowTier: PriceTierStats,
    val midTier: PriceTierStats,
    val highTier: PriceTierStats
)

object PriceHeuristicEngine {
    fun analyze(records: List<RideRecord>): PriceRecommendation {
        val total = records.size
        val accepted = records.filter { it.status.equals("ACCEPTED", ignoreCase = true) }
        val rejected = records.filter { it.status.equals("REJECTED", ignoreCase = true) || it.status.equals("IGNORED", ignoreCase = true) }

        // Low tier: fare < ₹80
        val lowList = records.filter { it.fare < 80f }
        val lowAccepted = lowList.count { it.status.equals("ACCEPTED", ignoreCase = true) }
        val lowRejected = lowList.count { !it.status.equals("ACCEPTED", ignoreCase = true) }
        val lowRate = if (lowList.isNotEmpty()) (lowAccepted * 100) / lowList.size else 0
        val lowTier = PriceTierStats(
            tierName = "Low Tier",
            rangeLabel = "< ₹80",
            totalCount = lowList.size,
            acceptedCount = lowAccepted,
            rejectedCount = lowRejected,
            acceptRatePercent = lowRate
        )

        // Mid tier: fare between ₹80 and ₹220
        val midList = records.filter { it.fare in 80f..220f }
        val midAccepted = midList.count { it.status.equals("ACCEPTED", ignoreCase = true) }
        val midRejected = midList.count { !it.status.equals("ACCEPTED", ignoreCase = true) }
        val midRate = if (midList.isNotEmpty()) (midAccepted * 100) / midList.size else 0
        val midTier = PriceTierStats(
            tierName = "Mid Tier",
            rangeLabel = "₹80 - ₹220",
            totalCount = midList.size,
            acceptedCount = midAccepted,
            rejectedCount = midRejected,
            acceptRatePercent = midRate
        )

        // High tier: fare > ₹220
        val highList = records.filter { it.fare > 220f }
        val highAccepted = highList.count { it.status.equals("ACCEPTED", ignoreCase = true) }
        val highRejected = highList.count { !it.status.equals("ACCEPTED", ignoreCase = true) }
        val highRate = if (highList.isNotEmpty()) (highAccepted * 100) / highList.size else 0
        val highTier = PriceTierStats(
            tierName = "High Tier",
            rangeLabel = "> ₹220",
            totalCount = highList.size,
            acceptedCount = highAccepted,
            rejectedCount = highRejected,
            acceptRatePercent = highRate
        )

        if (total == 0) {
            return PriceRecommendation(
                suggestedMinPrice = 60f,
                suggestedMaxPrice = 350f,
                rationale = "No decision history recorded yet. Default baseline heuristic of ₹60 - ₹350 suggested to maximize order density while filtering unprofitable low fares.",
                confidenceBadge = "LEARNING (COLLECTING DATA)",
                totalDecisions = 0,
                acceptedDecisions = 0,
                rejectedDecisions = 0,
                lowTier = lowTier,
                midTier = midTier,
                highTier = highTier
            )
        }

        val confidence = when {
            total >= 15 -> "HIGH CONFIDENCE"
            total >= 5 -> "MODERATE CONFIDENCE"
            else -> "LOW CONFIDENCE (COLLECTING DATA)"
        }

        val suggestedMin = when {
            lowTier.totalCount >= 2 && lowTier.acceptRatePercent < 30 -> 80f
            lowTier.totalCount >= 2 && lowTier.acceptRatePercent < 60 -> 70f
            else -> 60f
        }

        val suggestedMax = when {
            highTier.acceptedCount >= 2 -> 450f
            midTier.acceptedCount >= 3 -> 350f
            else -> 300f
        }

        val rationale = buildString {
            if (lowTier.rejectedCount > lowTier.acceptedCount) {
                append("Driver frequently declines low fare orders (< ₹80: ${lowTier.rejectedCount} rejected). ")
            } else if (lowTier.totalCount > 0) {
                append("Driver shows willingness to accept short micro-trips. ")
            }
            if (midTier.acceptedCount > 0) {
                append("Mid-tier trips (₹80-₹220) have a strong ${midTier.acceptRatePercent}% acceptance rate. ")
            }
            if (highTier.acceptedCount > 0) {
                append("High fare rides (> ₹220) are prioritized (${highTier.acceptedCount} accepted). ")
            }
            append("Suggested range of ₹${suggestedMin.toInt()} - ₹${suggestedMax.toInt()} maximizes driver earnings per hour.")
        }

        return PriceRecommendation(
            suggestedMinPrice = suggestedMin,
            suggestedMaxPrice = suggestedMax,
            rationale = rationale,
            confidenceBadge = confidence,
            totalDecisions = total,
            acceptedDecisions = accepted.size,
            rejectedDecisions = rejected.size,
            lowTier = lowTier,
            midTier = midTier,
            highTier = highTier
        )
    }
}

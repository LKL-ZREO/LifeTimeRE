package com.lifetimere.app.domain

data class AwardedAction(val action: DetectedAction, val points: Int)

object GrowthRules {
    const val VERSION = 2
    const val MAX_PER_ATTRIBUTE = 3
    const val MAX_PER_DAY = 8

    // Intensity is bounded input from a recognizer; only this table determines points.
    private val pointsByIntensity = mapOf(1 to 1, 2 to 2, 3 to 3)

    fun settle(body: String, detected: DetectedReview): List<AwardedAction> {
        val usedIds = mutableSetOf<String>()
        val perAttribute = mutableMapOf<CharacterAttribute, Int>()
        var total = 0
        return detected.actions.mapNotNull { action ->
            require(action.id.isNotBlank() && usedIds.add(action.id)) { "Duplicate or empty action ID" }
            require(action.quote.isNotBlank() && body.contains(action.quote)) { "Evidence must quote the entry" }
            val requested = pointsByIntensity[action.intensity]
                ?: throw IllegalArgumentException("Unsupported intensity")
            val current = perAttribute[action.attribute] ?: 0
            val points = minOf(requested, MAX_PER_ATTRIBUTE - current, MAX_PER_DAY - total)
            if (points <= 0) return@mapNotNull null
            perAttribute[action.attribute] = current + points
            total += points
            AwardedAction(action, points)
        }
    }
}

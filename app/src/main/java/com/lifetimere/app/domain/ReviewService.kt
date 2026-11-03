package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry

data class DetectedAction(
    val id: String,
    val attribute: CharacterAttribute,
    val intensity: Int,
    val quote: String,
    val explanation: String,
)

data class ReviewAnalysisMetadata(
    val model: String = "",
    val promptVersion: String = "",
    val schemaVersion: String = "",
    val analyzedAtEpochMillis: Long = 0,
)

data class DetectedReview(
    val summary: String,
    val actions: List<DetectedAction>,
    val metadata: ReviewAnalysisMetadata = ReviewAnalysisMetadata(),
)

enum class ReviewFeedbackReason(val label: String) {
    EVIDENCE_MISMATCH("依据与原文不符"),
    MISSED_OR_DUPLICATED_ACTION("遗漏或重复了行动"),
    ATTRIBUTE_MISMATCH("属性判断不合适"),
}

interface ReviewService {
    suspend fun analyze(entry: DailyEntry): DetectedReview
}

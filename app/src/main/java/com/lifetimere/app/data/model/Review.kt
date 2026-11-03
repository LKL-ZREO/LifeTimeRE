package com.lifetimere.app.data.model

import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.ReviewAnalysisMetadata

enum class ReviewStatus { PENDING, RUNNING, COMPLETE, FAILED }

data class ReviewEvidence(
    val actionId: String,
    val attribute: CharacterAttribute,
    val intensity: Int,
    val quote: String,
    val explanation: String,
    val points: Int,
)

data class Review(
    val entryId: Long,
    val revision: Int,
    val status: ReviewStatus,
    val summary: String,
    val evidence: List<ReviewEvidence>,
    val ruleVersion: Int,
    val metadata: ReviewAnalysisMetadata,
)

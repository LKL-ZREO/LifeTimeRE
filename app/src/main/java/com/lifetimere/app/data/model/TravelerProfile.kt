package com.lifetimere.app.data.model

import com.lifetimere.app.domain.CharacterAttribute

enum class TravelerProfileStatus {
    IN_PROGRESS,
    RETAKING,
    COMPLETED,
    SKIPPED,
    LEGACY_NEUTRAL,
}

data class TravelerProfile(
    val status: TravelerProfileStatus,
    val returnStatus: TravelerProfileStatus? = null,
    val questionnaireVersion: Int,
    val questionIndex: Int,
    val answers: Map<String, String>,
    val tendencies: Map<CharacterAttribute, Int>,
    val updatedAtEpochMillis: Long,
)

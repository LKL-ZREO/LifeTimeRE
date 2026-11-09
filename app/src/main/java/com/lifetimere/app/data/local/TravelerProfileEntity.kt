package com.lifetimere.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.model.TravelerProfileStatus
import com.lifetimere.app.domain.CharacterAttribute

@Entity(tableName = "traveler_profiles")
data class TravelerProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val status: String,
    val returnStatus: String? = null,
    val questionnaireVersion: Int,
    val questionIndex: Int,
    val strengthTendency: Int = NEUTRAL_TENDENCY,
    val enduranceTendency: Int = NEUTRAL_TENDENCY,
    val knowledgeTendency: Int = NEUTRAL_TENDENCY,
    val skillTendency: Int = NEUTRAL_TENDENCY,
    val resilienceTendency: Int = NEUTRAL_TENDENCY,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toModel(answers: Map<String, String>) = TravelerProfile(
        status = TravelerProfileStatus.valueOf(status),
        returnStatus = returnStatus?.let(TravelerProfileStatus::valueOf),
        questionnaireVersion = questionnaireVersion,
        questionIndex = questionIndex,
        answers = answers,
        tendencies = mapOf(
            CharacterAttribute.STRENGTH to strengthTendency,
            CharacterAttribute.KNOWLEDGE to knowledgeTendency,
            CharacterAttribute.CREATION to skillTendency,
            CharacterAttribute.SPIRIT to resilienceTendency,
        ),
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        const val SINGLETON_ID = 1
        const val NEUTRAL_TENDENCY = 3
    }
}

@Entity(
    tableName = "traveler_answers",
    primaryKeys = ["questionId"],
    foreignKeys = [
        ForeignKey(
            entity = TravelerProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class TravelerAnswerEntity(
    val questionId: String,
    val profileId: Int = TravelerProfileEntity.SINGLETON_ID,
    val optionId: String,
    val questionnaireVersion: Int,
)

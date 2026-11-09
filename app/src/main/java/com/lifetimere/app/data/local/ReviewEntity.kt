package com.lifetimere.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    foreignKeys = [ForeignKey(entity = DailyEntryEntity::class, parentColumns = ["id"], childColumns = ["entry_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("entry_id")],
)
data class ReviewEntity(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: Long,
    val revision: Int,
    val status: String,
    val summary: String = "",
    @ColumnInfo(name = "rule_version") val ruleVersion: Int = 1,
    @ColumnInfo(name = "analysis_model") val analysisModel: String = "",
    @ColumnInfo(name = "prompt_version") val promptVersion: String = "",
    @ColumnInfo(name = "schema_version") val schemaVersion: String = "",
    @ColumnInfo(name = "analyzed_at_epoch_millis") val analyzedAtEpochMillis: Long = 0,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "last_attempt_status") val lastAttemptStatus: String = "NONE",
    @ColumnInfo(name = "last_error_kind") val lastErrorKind: String = "",
    @ColumnInfo(name = "last_error_message") val lastErrorMessage: String = "",
    @ColumnInfo(name = "last_attempt_at_epoch_millis") val lastAttemptAtEpochMillis: Long = 0,
)

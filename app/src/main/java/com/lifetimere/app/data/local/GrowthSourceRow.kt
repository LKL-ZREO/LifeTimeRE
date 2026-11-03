package com.lifetimere.app.data.local

import androidx.room.ColumnInfo

data class GrowthSourceRow(
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    val body: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    val revision: Int,
    @ColumnInfo(name = "review_summary") val reviewSummary: String,
    @ColumnInfo(name = "rule_version") val ruleVersion: Int,
    @ColumnInfo(name = "analysis_model") val analysisModel: String,
    @ColumnInfo(name = "prompt_version") val promptVersion: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: String,
    @ColumnInfo(name = "analyzed_at_epoch_millis") val analyzedAtEpochMillis: Long,
    @ColumnInfo(name = "action_id") val actionId: String?,
    @ColumnInfo(name = "attribute_id") val attributeId: String?,
    val intensity: Int?,
    val quote: String?,
    val explanation: String?,
    val points: Int?,
)

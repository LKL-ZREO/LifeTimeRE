package com.lifetimere.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifetimere.app.data.model.DailyEntry
import java.time.LocalDate

@Entity(
    tableName = "daily_entries",
    indices = [Index(value = ["local_date"], unique = true)],
)
data class DailyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    val body: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    val revision: Int,
)

fun DailyEntryEntity.toModel() = DailyEntry(
    id = id,
    localDate = LocalDate.parse(localDate),
    timeZoneId = timeZoneId,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    revision = revision,
)

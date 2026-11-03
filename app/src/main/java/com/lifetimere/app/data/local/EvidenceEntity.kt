package com.lifetimere.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "evidence",
    primaryKeys = ["entry_id", "action_id"],
    foreignKeys = [ForeignKey(entity = DailyEntryEntity::class, parentColumns = ["id"], childColumns = ["entry_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("entry_id")],
)
data class EvidenceEntity(
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "action_id") val actionId: String,
    val revision: Int,
    @ColumnInfo(name = "attribute_id") val attributeId: String,
    val intensity: Int,
    val quote: String,
    val explanation: String,
)

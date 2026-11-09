package com.lifetimere.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keepsakes",
    foreignKeys = [ForeignKey(
        entity = DailyEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("entryId"), Index(value = ["shelfSlot"], unique = true), Index(value = ["chapterSlot"], unique = true)],
)
data class KeepsakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val quote: String,
    val title: String,
    val material: String,
    val shelfSlot: Int? = null,
    val chapterSlot: Int? = null,
)

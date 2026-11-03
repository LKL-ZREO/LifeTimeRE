package com.lifetimere.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_notes",
    foreignKeys = [ForeignKey(
        entity = DailyEntryEntity::class,
        parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("entryId")],
)
data class JournalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun List<JournalNoteEntity>.journalBody(): String = joinToString("\n\n") { it.body }

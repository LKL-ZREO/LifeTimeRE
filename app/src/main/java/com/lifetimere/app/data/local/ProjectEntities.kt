package com.lifetimere.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nextStep: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "project_milestones",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class ProjectMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "project_entries",
    primaryKeys = ["projectId", "entryId"],
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DailyEntryEntity::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("entryId")],
)
data class ProjectEntryEntity(val projectId: Long, val entryId: Long)

@Entity(
    tableName = "action_followups",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class ActionFollowupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val action: String,
    val dueDate: String,
    val status: String = "PENDING",
    val outcome: String = "",
    val completedAt: Long? = null,
)

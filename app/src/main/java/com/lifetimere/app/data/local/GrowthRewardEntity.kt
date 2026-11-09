package com.lifetimere.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "growth_rewards",
    foreignKeys = [ForeignKey(
        entity = DailyEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["sourceEntryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("sourceEntryId")],
)
data class GrowthRewardEntity(
    @PrimaryKey val rewardId: String,
    val sourceEntryId: Long?,
)

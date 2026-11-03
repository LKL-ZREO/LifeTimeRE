package com.lifetimere.app.data.model

import java.time.LocalDate

data class DailyEntry(
    val id: Long,
    val localDate: LocalDate,
    val timeZoneId: String,
    val body: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Int,
)

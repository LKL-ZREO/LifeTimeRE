package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeeklyReview(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val recordedDays: Int,
    val reviewedDays: Int,
    val pendingDays: Int,
    val totalGrowth: Int,
    val items: List<WeeklyReviewItem>,
)

data class WeeklyReviewItem(
    val entryId: Long,
    val date: LocalDate,
    val summary: String,
    val quotes: List<String>,
    val fullBody: String,
    val growthPoints: Int,
)

object WeeklyReviewBuilder {
    fun build(today: LocalDate, entries: List<DailyEntry>, sources: List<GrowthSource>): WeeklyReview {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        val weekEntries = entries.filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(end) }
            .sortedByDescending { it.localDate }
        val currentEntries = weekEntries.associateBy { it.id }
        val settledSources = sources.filter { source ->
            val current = currentEntries[source.entry.id]
            current != null && current.body == source.entry.body
        }
        val items = settledSources.sortedByDescending { it.entry.localDate }.mapNotNull { source ->
            val entry = currentEntries[source.entry.id] ?: return@mapNotNull null
            val evidenceQuotes = source.evidence.map { it.quote }
                .filter { entry.body.contains(it) }
                .distinct()
                .take(2)
            val quotes = evidenceQuotes.ifEmpty { listOf(entry.body.take(100)) }
            WeeklyReviewItem(
                entryId = entry.id,
                date = entry.localDate,
                summary = source.summary.ifBlank { "这一天留下了一段旅程记录。" },
                quotes = quotes,
                fullBody = entry.body,
                growthPoints = source.evidence.sumOf { it.points },
            )
        }
        return WeeklyReview(
            startDate = start,
            endDate = end,
            recordedDays = weekEntries.map { it.localDate }.distinct().size,
            reviewedDays = settledSources.map { it.entry.localDate }.distinct().size,
            pendingDays = (weekEntries.size - settledSources.size).coerceAtLeast(0),
            totalGrowth = items.sumOf { it.growthPoints },
            items = items,
        )
    }
}

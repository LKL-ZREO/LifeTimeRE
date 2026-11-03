package com.lifetimere.app.domain

import com.lifetimere.app.data.LifeTimeRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DailySettlementTime {
    fun dueAt(
        date: LocalDate,
        zone: ZoneId,
        minuteOfDay: Int = AiReviewSettings.DEFAULT_SETTLEMENT_MINUTE_OF_DAY,
    ): Instant = date.atStartOfDay().plusMinutes(minuteOfDay.toLong()).atZone(zone).toInstant()

    fun delayMillis(
        date: LocalDate,
        zone: ZoneId,
        now: Instant,
        minuteOfDay: Int = AiReviewSettings.DEFAULT_SETTLEMENT_MINUTE_OF_DAY,
    ): Long = (dueAt(date, zone, minuteOfDay).toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)
}

enum class SettlementOutcome { COMPLETED, ALREADY_COMPLETE, STALE }

/** A shared path for manual and background settlement; no AI request runs in a DB transaction. */
class DailySettlement(
    private val repository: LifeTimeRepository,
    private val service: () -> ReviewService,
) {
    private val lock = Mutex()
    private val activeMutable = MutableStateFlow<Set<Long>>(emptySet())
    val active = activeMutable.asStateFlow()

    suspend fun settle(entryId: Long, force: Boolean = false): SettlementOutcome = lock.withLock {
        val snapshot = repository.journalSnapshot(entryId, force) ?: return@withLock SettlementOutcome.ALREADY_COMPLETE
        activeMutable.value = setOf(entryId)
        try {
            val result = service().analyze(snapshot.entry)
            if (repository.settleJournal(snapshot, result)) SettlementOutcome.COMPLETED else SettlementOutcome.STALE
        } finally {
            activeMutable.value = emptySet()
        }
    }
}

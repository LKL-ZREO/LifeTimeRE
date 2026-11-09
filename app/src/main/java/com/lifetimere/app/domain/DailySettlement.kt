package com.lifetimere.app.domain

import com.lifetimere.app.data.LifeTimeRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
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
        if (!repository.beginSettlementAttempt(snapshot)) return@withLock SettlementOutcome.STALE
        activeMutable.value = setOf(entryId)
        try {
            val result = service().analyze(snapshot.entry)
            if (repository.settleJournal(snapshot, result)) SettlementOutcome.COMPLETED else SettlementOutcome.STALE
        } catch (error: CancellationException) {
            repository.cancelSettlementAttempt(snapshot)
            throw error
        } catch (error: Exception) {
            val failure = ReviewFailure.from(error)
            repository.failSettlementAttempt(snapshot, failure.kind, failure.message)
            throw error
        } finally {
            activeMutable.value = emptySet()
        }
    }
}

data class ReviewFailure(val kind: String, val message: String) {
    companion object {
        fun from(error: Exception): ReviewFailure = when (error) {
            is SocketTimeoutException -> ReviewFailure("TIMEOUT", "AI 服务响应超时，请稍后重试。")
            is IOException -> ReviewFailure("NETWORK", "网络连接失败，请检查网络后重试。")
            is AiReviewException -> ReviewFailure("SERVICE", error.message?.takeIf { it.isNotBlank() }
                ?: "AI 服务暂时不可用，请稍后重试。")
            is IllegalArgumentException -> ReviewFailure("INVALID_RESPONSE", "AI 返回的内容未通过校验，请重新汇总。")
            else -> ReviewFailure("UNKNOWN", "汇总没有完成，请稍后重试。")
        }
    }
}

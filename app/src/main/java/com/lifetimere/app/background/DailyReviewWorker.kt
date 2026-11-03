package com.lifetimere.app.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lifetimere.app.LifeTimeApplication
import com.lifetimere.app.domain.DailySettlementTime
import com.lifetimere.app.domain.SettlementOutcome
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class DailyReviewWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LifeTimeApplication
        if (!app.isAiAvailable()) return Result.success()
        val id = inputData.getLong(ENTRY_ID, -1)
        val snapshot = app.repository.journalSnapshot(id) ?: return Result.success()
        val zone = runCatching { ZoneId.of(snapshot.entry.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val settlementMinute = app.aiSettingsStore.settings.value.settlementMinuteOfDay
        if (Instant.now().isBefore(DailySettlementTime.dueAt(snapshot.entry.localDate, zone, settlementMinute))) {
            return Result.retry()
        }
        return try {
            when (app.dailySettlement.settle(id)) {
                SettlementOutcome.STALE -> Result.retry()
                else -> Result.success()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Leave the notes and previous successful settlement intact. WorkManager backs off.
            Result.retry()
        }
    }

    companion object { const val ENTRY_ID = "entry_id" }
}

class ReviewRecoveryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as LifeTimeApplication).reviewScheduler.schedulePending()
        return Result.success()
    }
}

class DailyReviewScheduler(private val context: Context) {
    private val manager get() = WorkManager.getInstance(context)

    fun startRecovery() {
        manager.enqueueUniquePeriodicWork(
            "daily-review-recovery", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReviewRecoveryWorker>(1, TimeUnit.HOURS).build(),
        )
    }

    suspend fun schedulePending() {
        val app = context.applicationContext as LifeTimeApplication
        if (!app.isAiAvailable()) return
        enqueuePending(app.repository.pendingJournalIds(), ExistingWorkPolicy.KEEP)
    }

    suspend fun reschedulePending() {
        val app = context.applicationContext as LifeTimeApplication
        val pending = app.repository.pendingJournalIds()
        if (!app.isAiAvailable()) {
            pending.forEach { manager.cancelUniqueWork(workName(it)) }
            return
        }
        enqueuePending(pending, ExistingWorkPolicy.REPLACE)
    }

    private suspend fun enqueuePending(entryIds: List<Long>, policy: ExistingWorkPolicy) {
        val app = context.applicationContext as LifeTimeApplication
        val settlementMinute = app.aiSettingsStore.settings.value.settlementMinuteOfDay
        entryIds.forEach { id ->
            val snapshot = app.repository.journalSnapshot(id) ?: return@forEach
            val zone = runCatching { ZoneId.of(snapshot.entry.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
            val request = OneTimeWorkRequestBuilder<DailyReviewWorker>()
                .setInputData(workDataOf(DailyReviewWorker.ENTRY_ID to id))
                .setInitialDelay(
                    DailySettlementTime.delayMillis(snapshot.entry.localDate, zone, Instant.now(), settlementMinute),
                    TimeUnit.MILLISECONDS,
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            manager.enqueueUniqueWork(workName(id), policy, request)
        }
    }

    private fun workName(entryId: Long) = "daily-review-$entryId"
}

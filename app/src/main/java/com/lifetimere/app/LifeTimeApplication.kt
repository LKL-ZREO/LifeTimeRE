package com.lifetimere.app

import android.app.Application
import com.lifetimere.app.data.local.LifeTimeDatabase
import com.lifetimere.app.data.LifeTimeRepository
import com.lifetimere.app.background.DailyReviewScheduler
import com.lifetimere.app.domain.AiReviewSettingsStore
import com.lifetimere.app.domain.AiConnectionMode
import com.lifetimere.app.domain.DailySettlement
import com.lifetimere.app.domain.DirectDeepSeekReviewService
import com.lifetimere.app.domain.RemoteReviewService
import com.lifetimere.app.domain.ReviewService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class LifeTimeApplication : Application() {
    val database: LifeTimeDatabase by lazy { LifeTimeDatabase.create(this) }
    val repository: LifeTimeRepository by lazy { LifeTimeRepository(database) }
    val aiSettingsStore by lazy { AiReviewSettingsStore(this) }
    val gatewayReviewService by lazy { RemoteReviewService { aiSettingsStore.settings.value } }
    val directReviewService by lazy { DirectDeepSeekReviewService { aiSettingsStore.settings.value.accessToken } }
    val reviewService: ReviewService get() = if (aiSettingsStore.settings.value.mode == AiConnectionMode.DEEPSEEK) directReviewService else gatewayReviewService
    val dailySettlement by lazy { DailySettlement(repository) { reviewService } }
    val reviewScheduler by lazy { DailyReviewScheduler(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isAiAvailable() = aiSettingsStore.settings.value.isReady

    override fun onCreate() {
        super.onCreate()
        reviewScheduler.startRecovery()
        appScope.launch { repository.recoverInterruptedAttempts() }
        appScope.launch {
            repository.observeNotes().collect {
                reviewScheduler.schedulePending()
            }
        }
        appScope.launch {
            aiSettingsStore.settings.drop(1).collect { reviewScheduler.reschedulePending() }
        }
    }

    fun recoverReviews() { appScope.launch { reviewScheduler.schedulePending() } }
    fun rescheduleReviews() { appScope.launch { reviewScheduler.reschedulePending() } }
}

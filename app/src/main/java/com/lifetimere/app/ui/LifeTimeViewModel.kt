package com.lifetimere.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifetimere.app.LifeTimeApplication
import com.lifetimere.app.data.LifeTimeRepository
import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.Review
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.data.model.ReviewAttemptStatus
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.local.JournalNoteEntity
import com.lifetimere.app.data.local.journalBody
import com.lifetimere.app.domain.SettlementOutcome
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.CampState
import com.lifetimere.app.domain.AiReviewSettings
import com.lifetimere.app.domain.AiConnectionMode
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.domain.TravelerQuestionnaire
import com.lifetimere.app.domain.WeeklyReview
import com.lifetimere.app.domain.WeeklyReviewBuilder
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class TodayUiState(
    val draft: String = "",
    val savedEntry: DailyEntry? = null,
    val review: Review? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val isAnalyzing: Boolean = false,
    val notice: String? = null,
    val dateLabel: String = "",
    val aiConfigured: Boolean = false,
    val notes: List<JournalNoteEntity> = emptyList(),
    val settlementTimeLabel: String = "22:00",
    val savedFootprint: SavedFootprintUi? = null,
) {
    val hasPendingNotes: Boolean get() = notes.isNotEmpty() &&
        (notes.journalBody() != savedEntry?.body || review?.status != ReviewStatus.COMPLETE ||
            review?.attemptStatus in setOf(ReviewAttemptStatus.RUNNING, ReviewAttemptStatus.FAILED))
    val canSave: Boolean
        get() = isLoaded && !isSaving && draft.isNotBlank()

    val buttonLabel: String
        get() = when {
            !isLoaded -> "正在读取旅志"
            isSaving -> "正在保存"
            draft.isBlank() -> "写下这一刻"
            else -> "记下这一笔"
        }
}

data class SavedFootprintUi(
    val preview: String,
    val noteCount: Int,
)

data class AiConnectionUiState(
    val isTesting: Boolean = false,
    val successful: Boolean? = null,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LifeTimeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LifeTimeRepository =
        (application as LifeTimeApplication).repository
    private val app = application as LifeTimeApplication
    private val aiSettingsStore = app.aiSettingsStore
    val aiSettings: StateFlow<AiReviewSettings> = aiSettingsStore.settings
    private val gatewayReviewService = app.gatewayReviewService
    private val directDeepSeekReviewService = app.directReviewService
    private val aiConnectionStateMutable = MutableStateFlow(AiConnectionUiState())
    val aiConnectionState: StateFlow<AiConnectionUiState> = aiConnectionStateMutable
    private val inFlight = mutableSetOf<Pair<Long, Int>>()
    private val activeReviews = MutableStateFlow<Set<Pair<Long, Int>>>(emptySet())
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val todayDate get() = currentDate.value

    private val draft = MutableStateFlow("")
    private val todayEntry = MutableStateFlow<DailyEntry?>(null)
    private val isTodayLoaded = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)
    private val savedFootprint = MutableStateFlow<SavedFootprintUi?>(null)
    private val firstCampEchoEventsMutable = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val firstCampEchoEvents = firstCampEchoEventsMutable.asSharedFlow()
    private val worldFeedbackPreferences = application.getSharedPreferences("world_feedback", Application.MODE_PRIVATE)
    private val firstCampEchoLock = Any()

    private val todayReview = todayEntry.flatMapLatest { entry ->
        if (entry == null) flowOf(null) else repository.observeReview(entry.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val todayNotes = combine(repository.observeNotes(), todayEntry) { notes, entry ->
        notes.filter { it.entryId == entry?.id }
    }

    private val todayFeedback = combine(notice, savedFootprint) { currentNotice, footprint ->
        currentNotice to footprint
    }

    private val todayBaseState = combine(
        draft,
        todayEntry,
        isTodayLoaded,
        isSaving,
        todayFeedback,
    ) { currentDraft, entry, loaded, saving, feedback ->
        TodayUiState(
            draft = currentDraft,
            savedEntry = entry,
            isLoaded = loaded,
            isSaving = saving,
            notice = feedback.first,
            savedFootprint = feedback.second,
            dateLabel = formatDate(todayDate),
        )
    }

    private val reviewActivity = combine(activeReviews, app.dailySettlement.active) { manual, background ->
        manual.map { it.first }.toSet() + background
    }
    val todayUiState: StateFlow<TodayUiState> = combine(todayBaseState, todayReview, reviewActivity, aiSettings, todayNotes) { state, review, active, settings, notes ->
        state.copy(
            review = review?.takeIf { it.revision == state.savedEntry?.revision },
            isAnalyzing = state.savedEntry?.id in active,
            aiConfigured = settings.isReady,
            notes = notes,
            settlementTimeLabel = settings.settlementTimeLabel,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(dateLabel = formatDate(todayDate)),
    )

    val entries: StateFlow<List<DailyEntry>> = repository.observeJournalEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val journeyDirection: StateFlow<String?> = repository.observeJourneyDirection().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val attributeTotals: StateFlow<Map<CharacterAttribute, Int>> = repository.observeAttributeTotals().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val reviewStatuses: StateFlow<Map<Long, ReviewStatus>> = repository.observeJournalStatuses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val reviewFailures: StateFlow<Map<Long, String>> = repository.observeReviewFailures().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val growthRewards = repository.observeGrowthRewards().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    val growthSources: StateFlow<List<GrowthSource>> = repository.observeGrowthSources().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val actionFollowups = repository.observeActionFollowups().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    suspend fun createActionFollowup(projectId: Long, action: String, date: LocalDate) =
        repository.createActionFollowup(projectId, action, date)
    suspend fun rescheduleActionFollowup(id: Long, date: LocalDate) =
        repository.rescheduleActionFollowup(id, date)
    suspend fun completeActionFollowup(id: Long, status: String, outcome: String) =
        repository.completeActionFollowup(id, status, outcome)

    val projects = repository.observeProjects().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val projectMilestones = repository.observeProjectMilestones().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val projectEntries = repository.observeProjectEntries().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    suspend fun createProject(name: String) = repository.createProject(name)
    suspend fun addProjectMilestone(id: Long, title: String) = repository.addProjectMilestone(id, title)
    suspend fun setMilestoneComplete(id: Long, complete: Boolean) = repository.setMilestoneComplete(id, complete)
    suspend fun setProjectNextStep(id: Long, nextStep: String) = repository.setProjectNextStep(id, nextStep)
    suspend fun setProjectEntry(id: Long, entryId: Long, linked: Boolean) =
        repository.setProjectEntry(id, entryId, linked)

    val worldDate: StateFlow<LocalDate> = currentDate
    val keepsakes = repository.observeKeepsakes().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    suspend fun displayKeepsake(id: Long?, chapter: Boolean): Boolean = repository.displayKeepsake(id, chapter)

    val weeklyReview: StateFlow<WeeklyReview> = combine(entries, growthSources, currentDate) { journal, sources, date ->
        WeeklyReviewBuilder.build(date, journal, sources)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklyReviewBuilder.build(LocalDate.now(), emptyList(), emptyList()),
    )

    val campState: StateFlow<CampState> = repository.observeCampState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampState(),
    )

    private val profileLoaded = MutableStateFlow(false)
    private val travelerProfileState = MutableStateFlow<TravelerProfile?>(null)
    val travelerProfile: StateFlow<TravelerProfile?> = travelerProfileState
    val isTravelerProfileLoaded: StateFlow<Boolean> = profileLoaded

    init {
        viewModelScope.launch {
            repository.observeTravelerProfile().collect { profile ->
                travelerProfileState.value = profile
                profileLoaded.value = true
            }
        }
        viewModelScope.launch {
            currentDate.flatMapLatest { date ->
                isTodayLoaded.value = false
                repository.observeEntry(date)
            }.collect { entry ->
                todayEntry.value = entry
                isTodayLoaded.value = true
            }
        }
        viewModelScope.launch {
            while (true) {
                refreshDate()
                delay(30_000)
            }
        }
    }

    fun refreshDate() { currentDate.value = LocalDate.now() }

    fun updateDraft(value: String) {
        draft.value = value.take(MAX_DRAFT_LENGTH)
        notice.value = null
    }

    fun saveToday() {
        val body = draft.value.trim()
        if (body.isEmpty() || isSaving.value) return
        refreshDate()
        val date = todayDate
        isSaving.value = true
        viewModelScope.launch {
            runCatching {
                repository.appendNote(
                    date = date, timeZoneId = ZoneId.systemDefault().id, body = body,
                    now = System.currentTimeMillis(),
                )
            }.onSuccess { saved ->
                if (draft.value.trim() == body) draft.value = ""
                if (date == todayDate) todayEntry.value = saved
                val count = repository.observeNotes().first().count { it.entryId == saved.id }
                savedFootprint.value = SavedFootprintUi(body.take(80), count)
                notice.value = if (isAiAvailable()) "已记下这一笔。每天 ${aiSettings.value.settlementTimeLabel} 后自动汇总；之后补记也会纳入当天结算。"
                    else "已保存在本机。配置 AI 服务后，将自动汇总当天记录。"
                app.reviewScheduler.schedulePending()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                notice.value = "保存失败：${error.message ?: "请稍后重试"}。文字仍保留在编辑区。"
            }
            isSaving.value = false
        }
    }

    fun updateEntry(id: Long, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.replaceJournal(id, body, System.currentTimeMillis())
                app.reviewScheduler.schedulePending()
            }.onFailure { notice.value = "修改失败：${it.message.orEmpty()}" }
        }
    }

    fun editNote(noteId: Long, body: String) {
        val entry = todayEntry.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.editNote(entry.id, noteId, body, System.currentTimeMillis())
                app.reviewScheduler.schedulePending()
            }.onFailure { notice.value = "修改失败：${it.message.orEmpty()}" }
        }
    }

    fun deleteNote(noteId: Long) {
        val entry = todayEntry.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.removeNote(entry.id, noteId)
                app.reviewScheduler.schedulePending()
            }.onFailure { notice.value = "删除失败，请稍后重试。" }
        }
    }

    fun summarizeToday() {
        refreshDate()
        val entry = todayEntry.value?.takeIf { it.localDate == todayDate } ?: return
        if (!isAiAvailable()) {
            notice.value = "请先在设置中配置 AI 服务。"
            return
        }
        if (draft.value.isNotBlank()) {
            notice.value = "输入框里还有未保存的文字，请先记下这一笔，再汇总。"
            return
        }
        analyze(entry)
    }

    fun summarizeEntry(entryId: Long) {
        if (!isAiAvailable()) { notice.value = "请先配置 AI 服务。"; return }
        entries.value.firstOrNull { it.id == entryId }?.let { analyze(it) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(id) }
        }
    }

    fun reportReview(entryId: Long, reason: ReviewFeedbackReason) {
        if (!isAiAvailable()) {
            notice.value = "异议尚未提交：请先在设置中启用并配置 AI 服务。"
            return
        }
        val entry = entries.value.firstOrNull { it.id == entryId }
        if (entry == null) {
            notice.value = "未找到这篇旅志，无法重新分析。"
            return
        }
        val key = entry.id to entry.revision
        if (!inFlight.add(key)) return
        activeReviews.value = activeReviews.value + key
        viewModelScope.launch {
            try {
                val current = repository.recordReviewFeedback(
                    entryId = entry.id,
                    reason = reason,
                    nowEpochMillis = System.currentTimeMillis(),
                ) ?: return@launch
                if (app.dailySettlement.settle(current.id, force = true) == SettlementOutcome.COMPLETED) {
                    notice.value = "已记录“${reason.label}”，并用新的 AI 回响替换原结果。"
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notice.value = "异议已保存在本机，但重新分析失败；原回响和属性仍然保留。${error.message.orEmpty()}"
            } finally {
                inFlight.remove(key)
                activeReviews.value = activeReviews.value - key
            }
        }
    }

    fun saveJourneyDirection(title: String) {
        viewModelScope.launch {
            repository.saveJourneyDirection(title, System.currentTimeMillis())
        }
    }

    fun saveAiSettings(settings: AiReviewSettings) {
        settings.validationError()?.let {
            aiConnectionStateMutable.value = AiConnectionUiState(successful = false, message = it)
            return
        }
        runCatching { aiSettingsStore.save(settings) }.onFailure {
            aiConnectionStateMutable.value = AiConnectionUiState(successful = false, message = "保存密钥失败，请重试。")
            return
        }
        app.rescheduleReviews()
        aiConnectionStateMutable.value = AiConnectionUiState(
            successful = true,
            message = when {
                settings.enabled -> "AI 服务已保存，每天 ${settings.settlementTimeLabel} 后自动汇总。"
                else -> "AI 回响已关闭；汇总时间已保存为 ${settings.settlementTimeLabel}。"
            },
        )
    }

    fun testAiConnection(settings: AiReviewSettings) {
        settings.validationError()?.let {
            aiConnectionStateMutable.value = AiConnectionUiState(successful = false, message = it)
            return
        }
        if (!settings.enabled) {
            aiConnectionStateMutable.value = AiConnectionUiState(successful = false, message = "请先启用 AI 回响。")
            return
        }
        if (settings.mode == AiConnectionMode.DEEPSEEK) {
            aiConnectionStateMutable.value = AiConnectionUiState(isTesting = true, message = "正在连接 DeepSeek……")
            viewModelScope.launch {
                runCatching { directDeepSeekReviewService.testConnection(settings.accessToken) }
                    .onSuccess { model ->
                        aiConnectionStateMutable.value = AiConnectionUiState(
                            successful = true,
                            message = "连接成功，服务模型：$model。",
                        )
                    }
                    .onFailure { error ->
                        aiConnectionStateMutable.value = AiConnectionUiState(
                            successful = false,
                            message = error.message ?: "无法连接 DeepSeek。",
                        )
                    }
            }
            return
        }
        aiConnectionStateMutable.value = AiConnectionUiState(isTesting = true, message = "正在连接 AI 服务……")
        viewModelScope.launch {
            runCatching { gatewayReviewService.testConnection(settings) }
                .onSuccess { model ->
                    aiConnectionStateMutable.value = AiConnectionUiState(
                        successful = true,
                        message = "连接成功，服务模型：$model。",
                    )
                }
                .onFailure { error ->
                    aiConnectionStateMutable.value = AiConnectionUiState(
                        successful = false,
                        message = error.message ?: "无法连接 AI 服务。",
                    )
                }
        }
    }

    fun beginTravelerRegistration(retake: Boolean = false) {
        viewModelScope.launch {
            repository.beginTravelerRegistration(retake, System.currentTimeMillis())
        }
    }

    fun chooseTravelerAnswer(questionId: String, optionId: String) {
        val profile = travelerProfile.value ?: return
        val questionIndex = profile.questionIndex.coerceIn(0, TravelerQuestionnaire.QUESTION_COUNT - 1)
        viewModelScope.launch {
            repository.saveTravelerAnswer(
                questionId = questionId,
                optionId = optionId,
                nextQuestionIndex = questionIndex + 1,
                nowEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun previousTravelerQuestion() {
        val profile = travelerProfile.value ?: return
        viewModelScope.launch {
            repository.setTravelerQuestionIndex(
                (profile.questionIndex - 1).coerceIn(0, TravelerQuestionnaire.QUESTION_COUNT),
                System.currentTimeMillis(),
            )
        }
    }

    fun completeTravelerRegistration() {
        viewModelScope.launch {
            repository.completeTravelerRegistration(System.currentTimeMillis())
        }
    }

    fun skipTravelerRegistration() {
        viewModelScope.launch {
            repository.skipTravelerRegistration(System.currentTimeMillis())
        }
    }

    private fun analyze(entry: DailyEntry) {
        val key = entry.id to entry.revision
        if (!inFlight.add(key)) return
        activeReviews.value = activeReviews.value + key
        viewModelScope.launch {
            try {
                val datesBefore = repository.observeCampState().first().settledDates
                val outcome = app.dailySettlement.settle(entry.id)
                val completed = outcome == SettlementOutcome.COMPLETED
                notice.value = when (outcome) {
                    SettlementOutcome.COMPLETED -> "全天总结已更新，属性已按当天总记录结算。"
                    SettlementOutcome.ALREADY_COMPLETE -> "当天记录已全部结算，没有重复加点。"
                    SettlementOutcome.STALE -> "汇总期间记录发生了变化，已安排重新汇总。"
                }
                if (outcome == SettlementOutcome.STALE) app.reviewScheduler.schedulePending()
                if (completed && datesBefore == 0 &&
                    travelerProfile.value?.status != com.lifetimere.app.data.model.TravelerProfileStatus.LEGACY_NEUTRAL
                ) {
                    val datesAfter = repository.observeCampState().first().settledDates
                    if (datesAfter > 0) synchronized(firstCampEchoLock) {
                        if (!worldFeedbackPreferences.getBoolean(FIRST_CAMP_ECHO_SEEN, false)) {
                            worldFeedbackPreferences.edit().putBoolean(FIRST_CAMP_ECHO_SEEN, true).apply()
                            firstCampEchoEventsMutable.tryEmit(Unit)
                        }
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notice.value = "记录已保存，汇总失败，原属性保留：${error.message ?: "请检查网络后重试。"}"
            } finally {
                inFlight.remove(key)
                activeReviews.value = activeReviews.value - key
            }
        }
    }

    private fun formatDate(date: LocalDate): String =
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    private fun isAiAvailable(): Boolean = aiSettings.value.isReady

    companion object {
        const val MAX_DRAFT_LENGTH = 5000
        private const val FIRST_CAMP_ECHO_SEEN = "first_camp_echo_seen"
    }
}

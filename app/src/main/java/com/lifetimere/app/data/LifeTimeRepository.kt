package com.lifetimere.app.data

import androidx.room.withTransaction
import com.lifetimere.app.data.local.DailyEntryEntity
import com.lifetimere.app.data.local.EvidenceEntity
import com.lifetimere.app.data.local.GrowthLedgerEntity
import com.lifetimere.app.data.local.JourneyDirectionEntity
import com.lifetimere.app.data.local.LifeTimeDatabase
import com.lifetimere.app.data.local.ReviewEntity
import com.lifetimere.app.data.local.ReviewFeedbackEntity
import com.lifetimere.app.data.local.TravelerAnswerEntity
import com.lifetimere.app.data.local.TravelerProfileEntity
import com.lifetimere.app.data.local.toModel
import com.lifetimere.app.data.local.JournalNoteEntity
import com.lifetimere.app.data.local.journalBody
import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.Review
import com.lifetimere.app.data.model.ReviewEvidence
import com.lifetimere.app.data.model.ReviewAttemptStatus
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.model.TravelerProfileStatus
import com.lifetimere.app.data.local.GrowthRewardEntity
import com.lifetimere.app.domain.GrowthReward
import com.lifetimere.app.domain.UnlockedGrowthReward
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.CampStage
import com.lifetimere.app.domain.CampState
import com.lifetimere.app.domain.DetectedReview
import com.lifetimere.app.domain.GrowthRules
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewAnalysisMetadata
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.domain.TravelerProfileScorer
import com.lifetimere.app.domain.TravelerQuestionnaire
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import com.lifetimere.app.data.local.KeepsakeEntity
import com.lifetimere.app.data.local.ActionFollowupEntity
import com.lifetimere.app.data.local.ProjectEntity
import com.lifetimere.app.data.local.ProjectEntryEntity
import com.lifetimere.app.data.local.ProjectMilestoneEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LifeTimeRepository(private val database: LifeTimeDatabase) {
    private val dao = database.dao()

    fun observeActionFollowups() = dao.observeActionFollowups()

    suspend fun createActionFollowup(projectId: Long, action: String, date: LocalDate): Boolean =
        database.withTransaction {
            if (dao.getProject(projectId) == null || action.isBlank()) return@withTransaction false
            dao.insertActionFollowup(ActionFollowupEntity(
                projectId = projectId, action = action.trim().take(240), dueDate = date.toString(),
            ))
            true
        }

    suspend fun rescheduleActionFollowup(id: Long, date: LocalDate): Boolean = database.withTransaction {
        val followup = dao.getActionFollowup(id) ?: return@withTransaction false
        if (followup.status != "PENDING") return@withTransaction false
        dao.rescheduleActionFollowup(id, date.toString())
        true
    }

    suspend fun completeActionFollowup(id: Long, status: String, outcome: String): Boolean =
        database.withTransaction {
            val followup = dao.getActionFollowup(id) ?: return@withTransaction false
            if (followup.status != "PENDING" || status !in setOf("DONE", "ADJUSTED", "DROPPED") ||
                outcome.isBlank()) return@withTransaction false
            dao.completeActionFollowup(id, status, outcome.trim().take(500), System.currentTimeMillis())
            true
        }

    fun observeProjects() = dao.observeProjects()
    fun observeProjectMilestones() = dao.observeProjectMilestones()
    fun observeProjectEntries() = dao.observeProjectEntries()

    suspend fun createProject(name: String): Long? =
        name.trim().takeIf { it.isNotEmpty() }?.let { dao.insertProject(ProjectEntity(name = it.take(80))) }

    suspend fun addProjectMilestone(projectId: Long, title: String): Boolean = database.withTransaction {
        if (dao.getProject(projectId) == null || title.isBlank()) return@withTransaction false
        dao.insertProjectMilestone(ProjectMilestoneEntity(projectId = projectId, title = title.trim().take(100)))
        true
    }

    suspend fun setMilestoneComplete(id: Long, complete: Boolean): Boolean = database.withTransaction {
        if (dao.getProjectMilestone(id) == null) return@withTransaction false
        dao.updateMilestoneCompletion(id, if (complete) System.currentTimeMillis() else null)
        true
    }

    suspend fun setProjectNextStep(id: Long, nextStep: String): Boolean = database.withTransaction {
        if (dao.getProject(id) == null) return@withTransaction false
        dao.updateProjectNextStep(id, nextStep.trim().take(240))
        true
    }

    suspend fun setProjectEntry(projectId: Long, entryId: Long, linked: Boolean): Boolean = database.withTransaction {
        if (dao.getProject(projectId) == null || (linked && dao.getEntry(entryId) == null)) return@withTransaction false
        if (linked) dao.linkProjectEntry(ProjectEntryEntity(projectId, entryId))
        else dao.unlinkProjectEntry(projectId, entryId)
        true
    }

    fun observeKeepsakes(): Flow<List<KeepsakeEntity>> = dao.observeKeepsakes()

    suspend fun craftKeepsake(entryId: Long, quote: String, title: String, material: String): Boolean =
        database.withTransaction {
            val entry = dao.getEntry(entryId) ?: return@withTransaction false
            val review = dao.getReview(entryId) ?: return@withTransaction false
            val notes = dao.getNotes(entryId)
            val body = if (notes.isEmpty()) entry.body else notes.journalBody()
            if (review.status != ReviewStatus.COMPLETE.name || review.revision != entry.revision ||
                body != entry.body || quote.isBlank() || quote !in body || title.isBlank()
            ) return@withTransaction false
            dao.insertKeepsake(KeepsakeEntity(entryId = entryId, quote = quote, title = title.trim(), material = material))
            true
        }

    suspend fun displayKeepsake(id: Long?, chapter: Boolean): Boolean = database.withTransaction {
        if (id != null) {
            val keepsake = dao.getKeepsake(id) ?: return@withTransaction false
            val entry = dao.getEntry(keepsake.entryId) ?: return@withTransaction false
            val notes = dao.getNotes(entry.id)
            val body = if (notes.isEmpty()) entry.body else notes.journalBody()
            if (keepsake.quote !in body) return@withTransaction false
        }
        if (chapter) {
            dao.clearChapterDisplay()
            if (id != null) dao.displayInChapter(id)
        } else {
            dao.clearShelf()
            if (id != null) dao.displayOnShelf(id)
        }
        true
    }

    fun observeNotes(): Flow<List<JournalNoteEntity>> = dao.observeNotes()

    // The daily entry is the last settled snapshot. Daytime notes are saved separately,
    // so an offline append never withdraws yesterday's or the last valid review's points.
    fun observeJournalEntries(): Flow<List<DailyEntry>> = combine(dao.observeEntries(), dao.observeNotes()) { entries, notes ->
        entries.map { entry ->
            val dayNotes = notes.filter { it.entryId == entry.id }
            entry.toModel().copy(body = if (dayNotes.isEmpty()) entry.body else dayNotes.journalBody())
        }
    }

    fun observeJournalStatuses(): Flow<Map<Long, ReviewStatus>> = combine(
        dao.observeEntries(), dao.observeNotes(), dao.observeReviews(),
    ) { entries, notes, reviews ->
        entries.associate { entry ->
            val body = notes.filter { it.entryId == entry.id }.let { if (it.isEmpty()) entry.body else it.journalBody() }
            val review = reviews.firstOrNull { it.entryId == entry.id }
            val pendingAttempt = review?.lastAttemptStatus?.let {
                runCatching { ReviewAttemptStatus.valueOf(it) }.getOrDefault(ReviewAttemptStatus.NONE)
            } ?: ReviewAttemptStatus.NONE
            entry.id to when (pendingAttempt) {
                ReviewAttemptStatus.RUNNING -> ReviewStatus.RUNNING
                ReviewAttemptStatus.FAILED -> ReviewStatus.FAILED
                ReviewAttemptStatus.NONE -> if (body != entry.body) ReviewStatus.PENDING
                    else review?.let { ReviewStatus.valueOf(it.status) } ?: ReviewStatus.PENDING
            }
        }
    }

    suspend fun appendNote(date: LocalDate, timeZoneId: String, body: String, now: Long): DailyEntry = database.withTransaction {
        val text = body.trim()
        require(text.isNotEmpty()) { "请先写下今天想留下的片段。" }
        val existing = dao.getEntry(date.toString())
        val entry = existing?.toModel() ?: saveEntry(date, timeZoneId, text, now)
        var notes = dao.getNotes(entry.id)
        if (existing != null && notes.isEmpty()) {
            dao.insertNote(JournalNoteEntity(entryId = entry.id, body = existing.body, createdAt = existing.createdAtEpochMillis, updatedAt = existing.updatedAtEpochMillis))
            notes = dao.getNotes(entry.id)
        }
        require((notes.map { it.body } + text).joinToString("\n\n").length <= MAX_DAY_LENGTH) {
            "今天的记录合计最多 $MAX_DAY_LENGTH 字，请先整理已有记录。"
        }
        dao.insertNote(JournalNoteEntity(entryId = entry.id, body = text, createdAt = now, updatedAt = now))
        resetAttempt(entry.id)
        entry
    }

    suspend fun editNote(entryId: Long, noteId: Long, body: String, now: Long) = database.withTransaction {
        val notes = dao.getNotes(entryId)
        val note = notes.firstOrNull { it.id == noteId } ?: return@withTransaction
        val text = body.trim()
        require(text.isNotEmpty()) { "记录不能为空。" }
        require(notes.map { if (it.id == noteId) it.copy(body = text) else it }.journalBody().length <= MAX_DAY_LENGTH) { "当天记录合计最多 $MAX_DAY_LENGTH 字。" }
        dao.updateNote(note.copy(body = text, updatedAt = now))
        resetAttempt(entryId)
    }

    suspend fun removeNote(entryId: Long, noteId: Long) = database.withTransaction {
        if (dao.getNotes(entryId).none { it.id == noteId }) return@withTransaction
        dao.deleteNote(noteId)
        if (dao.getNotes(entryId).isEmpty()) dao.getEntry(entryId)?.let { dao.deleteEntry(it) }
        else resetAttempt(entryId)
    }

    suspend fun replaceJournal(entryId: Long, body: String, now: Long) = database.withTransaction {
        val entry = dao.getEntry(entryId) ?: return@withTransaction
        require(body.isNotBlank() && body.trim().length <= MAX_DAY_LENGTH)
        dao.deleteNotes(entryId)
        dao.insertNote(JournalNoteEntity(entryId = entryId, body = body.trim(), createdAt = entry.createdAtEpochMillis, updatedAt = now))
        resetAttempt(entryId)
    }

    data class JournalSnapshot(val entry: DailyEntry, val baseRevision: Int, val notes: List<JournalNoteEntity>)

    suspend fun journalSnapshot(entryId: Long, force: Boolean = false): JournalSnapshot? = database.withTransaction {
        val entry = dao.getEntry(entryId) ?: return@withTransaction null
        val notes = dao.getNotes(entryId)
        val body = if (notes.isEmpty()) entry.body else notes.journalBody()
        val review = dao.getReview(entryId)
        if (!force && body == entry.body && review?.status == ReviewStatus.COMPLETE.name &&
            review.revision == entry.revision && review.lastAttemptStatus != ReviewAttemptStatus.FAILED.name
        ) return@withTransaction null
        JournalSnapshot(entry.toModel().copy(body = body, revision = entry.revision + 1), entry.revision, notes)
    }

    suspend fun pendingJournalIds(): List<Long> = dao.getEntries().mapNotNull { entry ->
        entry.id.takeIf { journalSnapshot(it) != null }
    }

    suspend fun beginSettlementAttempt(snapshot: JournalSnapshot): Boolean = database.withTransaction {
        val entry = dao.getEntry(snapshot.entry.id) ?: return@withTransaction false
        if (entry.revision != snapshot.baseRevision || dao.getNotes(entry.id) != snapshot.notes) return@withTransaction false
        val review = dao.getReview(entry.id) ?: ReviewEntity(
            entryId = entry.id,
            revision = entry.revision,
            status = ReviewStatus.PENDING.name,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        dao.upsertReview(review.copy(
            lastAttemptStatus = ReviewAttemptStatus.RUNNING.name,
            lastErrorKind = "",
            lastErrorMessage = "",
            lastAttemptAtEpochMillis = System.currentTimeMillis(),
        ))
        true
    }

    suspend fun failSettlementAttempt(snapshot: JournalSnapshot, kind: String, message: String) = database.withTransaction {
        val entry = dao.getEntry(snapshot.entry.id) ?: return@withTransaction
        if (entry.revision != snapshot.baseRevision || dao.getNotes(entry.id) != snapshot.notes) return@withTransaction
        dao.getReview(entry.id)?.let { review ->
            dao.upsertReview(review.copy(
                lastAttemptStatus = ReviewAttemptStatus.FAILED.name,
                lastErrorKind = kind,
                lastErrorMessage = message.take(180),
                lastAttemptAtEpochMillis = System.currentTimeMillis(),
            ))
        }
    }

    suspend fun cancelSettlementAttempt(snapshot: JournalSnapshot) = database.withTransaction {
        val entry = dao.getEntry(snapshot.entry.id) ?: return@withTransaction
        if (entry.revision != snapshot.baseRevision || dao.getNotes(entry.id) != snapshot.notes) return@withTransaction
        resetAttempt(entry.id)
    }

    suspend fun settleJournal(snapshot: JournalSnapshot, result: DetectedReview): Boolean = database.withTransaction {
        val old = dao.getEntry(snapshot.entry.id) ?: return@withTransaction false
        if (old.revision != snapshot.baseRevision || dao.getNotes(old.id) != snapshot.notes) return@withTransaction false
        val updated = old.copy(body = snapshot.entry.body, revision = snapshot.entry.revision, updatedAtEpochMillis = System.currentTimeMillis())
        dao.updateEntry(updated)
        persistReviewResult(updated, ReviewEntity(updated.id, updated.revision, ReviewStatus.PENDING.name, updatedAtEpochMillis = 0), result)
        true
    }


    fun observeEntry(date: LocalDate): Flow<DailyEntry?> =
        dao.observeEntry(date.toString()).map { it?.toModel() }

    fun observeEntries(): Flow<List<DailyEntry>> =
        dao.observeEntries().map { entries -> entries.map { it.toModel() } }

    fun observeJourneyDirection(): Flow<String?> =
        dao.observeJourneyDirection().map { it?.title }

    fun observeTravelerProfile(): Flow<TravelerProfile?> = combine(
        dao.observeTravelerProfile(),
        dao.observeTravelerAnswers(),
    ) { profile, answers ->
        profile?.toModel(answers.associate { it.questionId to it.optionId })
    }

    fun observeReview(entryId: Long): Flow<Review?> = combine(
        dao.observeReview(entryId),
        dao.observeEvidence(entryId),
        dao.observeGrowth(entryId),
    ) { review, evidence, growth ->
        review?.let { row ->
            val points = growth.associate { it.actionId to it.points }
            Review(
                entryId = row.entryId,
                revision = row.revision,
                status = ReviewStatus.valueOf(row.status),
                summary = row.summary,
                evidence = evidence.filter { it.revision == row.revision }.map {
                    ReviewEvidence(it.actionId, CharacterAttribute.valueOf(it.attributeId), it.intensity, it.quote, it.explanation, points[it.actionId] ?: 0)
                },
                ruleVersion = row.ruleVersion,
                metadata = ReviewAnalysisMetadata(
                    model = row.analysisModel,
                    promptVersion = row.promptVersion,
                    schemaVersion = row.schemaVersion,
                    analyzedAtEpochMillis = row.analyzedAtEpochMillis,
                ),
                attemptStatus = runCatching { ReviewAttemptStatus.valueOf(row.lastAttemptStatus) }
                    .getOrDefault(ReviewAttemptStatus.NONE),
                failureKind = row.lastErrorKind,
                failureMessage = row.lastErrorMessage,
                lastAttemptAtEpochMillis = row.lastAttemptAtEpochMillis,
            )
        }
    }

    fun observeReviewStatuses(): Flow<Map<Long, ReviewStatus>> =
        dao.observeReviews().map { reviews ->
            reviews.associate { it.entryId to ReviewStatus.valueOf(it.status) }
        }

    fun observeReviewFailures(): Flow<Map<Long, String>> = dao.observeReviews().map { reviews ->
        reviews.mapNotNull { review ->
            review.lastErrorMessage.takeIf { it.isNotBlank() }?.let { review.entryId to it }
        }.toMap()
    }

    suspend fun recoverInterruptedAttempts() = database.withTransaction {
        dao.getReviews().filter { it.lastAttemptStatus == ReviewAttemptStatus.RUNNING.name }.forEach { review ->
            dao.upsertReview(review.copy(
                lastAttemptStatus = ReviewAttemptStatus.FAILED.name,
                lastErrorKind = "INTERRUPTED",
                lastErrorMessage = "上次汇总被系统中断，请重新汇总。",
                lastAttemptAtEpochMillis = System.currentTimeMillis(),
            ))
        }
    }

    fun observeAttributeTotals(): Flow<Map<CharacterAttribute, Int>> =
        dao.observeEffectiveGrowth().map { growth ->
            growth.groupBy { CharacterAttribute.valueOf(it.attributeId) }
                .mapValues { (_, entries) -> entries.sumOf { it.points } }
        }

    fun observeGrowthRewards(): Flow<List<UnlockedGrowthReward>> =
        dao.observeGrowthRewards().map { rows ->
            rows.mapNotNull { row ->
                GrowthReward.entries.firstOrNull { it.id == row.rewardId }
                    ?.let { UnlockedGrowthReward(it, row.sourceEntryId) }
            }
        }

    fun observeGrowthSources(): Flow<List<GrowthSource>> =
        dao.observeEffectiveGrowthSources().map { rows ->
            rows.groupBy { it.entryId }.values.map { sourceRows ->
                val first = sourceRows.first()
                GrowthSource(
                    entry = DailyEntry(
                        id = first.entryId,
                        localDate = LocalDate.parse(first.localDate),
                        timeZoneId = first.timeZoneId,
                        body = first.body,
                        createdAtEpochMillis = first.createdAtEpochMillis,
                        updatedAtEpochMillis = first.updatedAtEpochMillis,
                        revision = first.revision,
                    ),
                    evidence = sourceRows.mapNotNull { row ->
                        val actionId = row.actionId ?: return@mapNotNull null
                        ReviewEvidence(
                            actionId = actionId,
                            attribute = CharacterAttribute.valueOf(row.attributeId ?: return@mapNotNull null),
                            intensity = row.intensity ?: return@mapNotNull null,
                            quote = row.quote ?: return@mapNotNull null,
                            explanation = row.explanation ?: return@mapNotNull null,
                            points = row.points ?: 0,
                        )
                    },
                    summary = first.reviewSummary,
                    ruleVersion = first.ruleVersion,
                    metadata = ReviewAnalysisMetadata(
                        model = first.analysisModel,
                        promptVersion = first.promptVersion,
                        schemaVersion = first.schemaVersion,
                        analyzedAtEpochMillis = first.analyzedAtEpochMillis,
                    ),
                )
            }.sortedByDescending { it.entry.localDate }
        }

    fun observeCampState(): Flow<CampState> = observeGrowthSources().map { sources ->
        val dates = sources.map { it.entry.localDate }.distinct().size
        CampState(
            stage = when {
                dates >= 3 -> CampStage.AWAKENED
                dates >= 1 -> CampStage.FIRST_ECHO
                else -> CampStage.DORMANT
            },
            settledDates = dates,
            sources = sources,
        )
    }

    suspend fun beginTravelerRegistration(retake: Boolean = false, nowEpochMillis: Long) =
        database.withTransaction {
            val existing = dao.getTravelerProfile()
            if (existing == null) {
                dao.upsertTravelerProfile(
                    TravelerProfileEntity(
                        status = TravelerProfileStatus.IN_PROGRESS.name,
                        questionnaireVersion = TravelerQuestionnaire.VERSION,
                        questionIndex = 0,
                        createdAtEpochMillis = nowEpochMillis,
                        updatedAtEpochMillis = nowEpochMillis,
                    ),
                )
                dao.clearTravelerAnswers()
            } else if (retake) {
                dao.clearTravelerAnswers()
                val alreadyInProgress = existing.status == TravelerProfileStatus.IN_PROGRESS.name ||
                    existing.status == TravelerProfileStatus.RETAKING.name
                dao.upsertTravelerProfile(
                    existing.copy(
                        status = if (alreadyInProgress) existing.status else TravelerProfileStatus.RETAKING.name,
                        returnStatus = if (alreadyInProgress) existing.returnStatus else existing.status,
                        questionnaireVersion = TravelerQuestionnaire.VERSION,
                        questionIndex = 0,
                        updatedAtEpochMillis = nowEpochMillis,
                    ),
                )
            }
        }

    suspend fun saveTravelerAnswer(questionId: String, optionId: String, nextQuestionIndex: Int, nowEpochMillis: Long) =
        database.withTransaction {
            val profile = dao.getTravelerProfile() ?: return@withTransaction
            require(profile.status == TravelerProfileStatus.IN_PROGRESS.name || profile.status == TravelerProfileStatus.RETAKING.name)
            require(TravelerQuestionnaire.findOption(questionId, optionId) != null) { "Unknown traveler answer" }
            require(nextQuestionIndex in 0..TravelerQuestionnaire.QUESTION_COUNT)
            dao.upsertTravelerAnswer(
                TravelerAnswerEntity(
                    questionId = questionId,
                    optionId = optionId,
                    questionnaireVersion = TravelerQuestionnaire.VERSION,
                ),
            )
            dao.upsertTravelerProfile(
                profile.copy(
                    questionnaireVersion = TravelerQuestionnaire.VERSION,
                    questionIndex = nextQuestionIndex,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
            )
        }

    suspend fun setTravelerQuestionIndex(index: Int, nowEpochMillis: Long) {
        require(index in 0..TravelerQuestionnaire.QUESTION_COUNT)
        database.withTransaction {
            dao.getTravelerProfile()?.let { profile ->
                if (profile.status == TravelerProfileStatus.IN_PROGRESS.name || profile.status == TravelerProfileStatus.RETAKING.name) {
                    dao.upsertTravelerProfile(profile.copy(questionIndex = index, updatedAtEpochMillis = nowEpochMillis))
                }
            }
        }
    }

    suspend fun completeTravelerRegistration(nowEpochMillis: Long): Boolean = database.withTransaction {
        val profile = dao.getTravelerProfile() ?: return@withTransaction false
        if (profile.status != TravelerProfileStatus.IN_PROGRESS.name && profile.status != TravelerProfileStatus.RETAKING.name) {
            return@withTransaction false
        }
        val answers = dao.getTravelerAnswers().associate { it.questionId to it.optionId }
        if (TravelerQuestionnaire.questions.any { it.id !in answers }) return@withTransaction false
        val tendencies = TravelerProfileScorer.score(answers)
        dao.upsertTravelerProfile(
            profile.copy(
                status = TravelerProfileStatus.COMPLETED.name,
                returnStatus = null,
                questionnaireVersion = TravelerQuestionnaire.VERSION,
                questionIndex = TravelerQuestionnaire.QUESTION_COUNT,
                strengthTendency = tendencies.getValue(CharacterAttribute.STRENGTH),
                knowledgeTendency = tendencies.getValue(CharacterAttribute.KNOWLEDGE),
                skillTendency = tendencies.getValue(CharacterAttribute.CREATION),
                resilienceTendency = tendencies.getValue(CharacterAttribute.SPIRIT),
                updatedAtEpochMillis = nowEpochMillis,
            ),
        )
        true
    }

    suspend fun skipTravelerRegistration(nowEpochMillis: Long) = database.withTransaction {
        val profile = dao.getTravelerProfile()
        if (profile?.status == TravelerProfileStatus.RETAKING.name) {
            dao.clearTravelerAnswers()
            dao.upsertTravelerProfile(
                profile.copy(
                    status = profile.returnStatus ?: TravelerProfileStatus.LEGACY_NEUTRAL.name,
                    returnStatus = null,
                    questionIndex = 0,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
            )
        } else {
            dao.clearTravelerAnswers()
            dao.upsertTravelerProfile(
                TravelerProfileEntity(
                    status = TravelerProfileStatus.SKIPPED.name,
                    questionnaireVersion = TravelerQuestionnaire.VERSION,
                    questionIndex = 0,
                    createdAtEpochMillis = profile?.createdAtEpochMillis ?: nowEpochMillis,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
            )
        }
    }

    suspend fun saveEntry(
        date: LocalDate,
        timeZoneId: String,
        body: String,
        nowEpochMillis: Long,
    ): DailyEntry = database.withTransaction {
        val normalizedBody = body.trim()
        require(normalizedBody.isNotEmpty()) { "Daily entry cannot be empty" }

        val existing = dao.getEntry(date.toString())
        if (existing == null) {
            val entity = DailyEntryEntity(
                localDate = date.toString(),
                timeZoneId = timeZoneId,
                body = normalizedBody,
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis,
                revision = 1,
            )
            val saved = entity.copy(id = dao.insertEntry(entity))
            invalidateReview(saved)
            saved.toModel()
        } else if (existing.body == normalizedBody) {
            existing.toModel()
        } else {
            val updated = existing.copy(
                timeZoneId = timeZoneId,
                body = normalizedBody,
                updatedAtEpochMillis = nowEpochMillis,
                revision = existing.revision + 1,
            )
            dao.updateEntry(updated)
            invalidateReview(updated)
            updated.toModel()
        }
    }

    suspend fun updateEntry(id: Long, body: String, nowEpochMillis: Long): DailyEntry? =
        database.withTransaction {
            val normalizedBody = body.trim()
            require(normalizedBody.isNotEmpty()) { "Daily entry cannot be empty" }
            val existing = dao.getEntry(id) ?: return@withTransaction null
            if (existing.body == normalizedBody) return@withTransaction existing.toModel()

            val updated = existing.copy(
                body = normalizedBody,
                updatedAtEpochMillis = nowEpochMillis,
                revision = existing.revision + 1,
            )
            dao.updateEntry(updated)
            invalidateReview(updated)
            updated.toModel()
        }

    suspend fun deleteEntry(id: Long) = database.withTransaction {
        dao.getEntry(id)?.let { dao.deleteEntry(it) }
    }

    private suspend fun invalidateReview(entry: DailyEntryEntity) {
        dao.deleteGrowth(entry.id)
        dao.deleteEvidence(entry.id)
        dao.upsertReview(ReviewEntity(entry.id, entry.revision, ReviewStatus.PENDING.name, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun beginReview(entryId: Long, revision: Int, retry: Boolean = false): DailyEntry? =
        database.withTransaction {
            val entry = dao.getEntry(entryId) ?: return@withTransaction null
            if (entry.revision != revision) return@withTransaction null
            val existing = dao.getReview(entryId)
            if (existing?.revision == revision && existing.status == ReviewStatus.COMPLETE.name) return@withTransaction null
            if (existing?.revision == revision && existing.status == ReviewStatus.RUNNING.name && !retry) return@withTransaction null
            dao.upsertReview(ReviewEntity(entryId, revision, ReviewStatus.RUNNING.name, updatedAtEpochMillis = System.currentTimeMillis()))
            entry.toModel()
        }

    suspend fun completeReview(entryId: Long, revision: Int, result: DetectedReview): Boolean =
        database.withTransaction {
            val entry = dao.getEntry(entryId) ?: return@withTransaction false
            val review = dao.getReview(entryId) ?: return@withTransaction false
            if (entry.revision != revision || review.revision != revision || review.status != ReviewStatus.RUNNING.name) return@withTransaction false
            persistReviewResult(entry, review, result)
            true
        }

    suspend fun recordReviewFeedback(
        entryId: Long,
        reason: ReviewFeedbackReason,
        nowEpochMillis: Long,
    ): DailyEntry? = database.withTransaction {
        val entry = dao.getEntry(entryId) ?: return@withTransaction null
        val review = dao.getReview(entryId) ?: return@withTransaction null
        if (review.status != ReviewStatus.COMPLETE.name || review.revision != entry.revision) return@withTransaction null
        dao.insertReviewFeedback(
            ReviewFeedbackEntity(
                entryId = entryId,
                revision = entry.revision,
                reason = reason.name,
                analysisModel = review.analysisModel,
                promptVersion = review.promptVersion,
                schemaVersion = review.schemaVersion,
                createdAtEpochMillis = nowEpochMillis,
            ),
        )
        entry.toModel()
    }

    suspend fun replaceCompleteReview(entryId: Long, revision: Int, result: DetectedReview): Boolean =
        database.withTransaction {
            val entry = dao.getEntry(entryId) ?: return@withTransaction false
            val review = dao.getReview(entryId) ?: return@withTransaction false
            if (entry.revision != revision || review.revision != revision || review.status != ReviewStatus.COMPLETE.name) {
                return@withTransaction false
            }
            persistReviewResult(entry, review, result)
            true
        }

    private suspend fun persistReviewResult(
        entry: DailyEntryEntity,
        review: ReviewEntity,
        result: DetectedReview,
    ) {
        require(result.summary.length <= 300) { "Review summary is too long" }
        val awards = GrowthRules.settle(entry.body, result)
        dao.deleteGrowth(entry.id)
        dao.deleteEvidence(entry.id)
        dao.insertEvidence(awards.map { (action, _) ->
            EvidenceEntity(entry.id, action.id, entry.revision, action.attribute.name, action.intensity, action.quote, action.explanation)
        })
        dao.insertGrowth(awards.map { (action, points) ->
            GrowthLedgerEntity(entry.id, action.id, entry.revision, action.attribute.name, points)
        })
        dao.upsertReview(
            review.copy(
                status = ReviewStatus.COMPLETE.name,
                summary = result.summary,
                ruleVersion = GrowthRules.VERSION,
                analysisModel = result.metadata.model,
                promptVersion = result.metadata.promptVersion,
                schemaVersion = result.metadata.schemaVersion,
                analyzedAtEpochMillis = result.metadata.analyzedAtEpochMillis,
                updatedAtEpochMillis = System.currentTimeMillis(),
                lastAttemptStatus = ReviewAttemptStatus.NONE.name,
                lastErrorKind = "",
                lastErrorMessage = "",
                lastAttemptAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        val affectedTracks = awards.mapNotNull { it.action.attribute.growthTrack }.distinct()
        for (track in affectedTracks) {
            val attributeIds = CharacterAttribute.entries.filter { it.growthTrack == track }.map { it.name }
            val points = dao.effectiveGrowthPoints(attributeIds)
            GrowthReward.entries.filter { it.attribute == track && points >= it.threshold }.forEach { reward ->
                dao.insertGrowthReward(GrowthRewardEntity(reward.id, entry.id))
            }
        }
    }

    suspend fun failReview(entryId: Long, revision: Int) = database.withTransaction {
        val current = dao.getReview(entryId)
        if (current?.revision == revision && current.status == ReviewStatus.RUNNING.name) {
            dao.upsertReview(current.copy(status = ReviewStatus.FAILED.name, updatedAtEpochMillis = System.currentTimeMillis()))
        }
    }

    private suspend fun resetAttempt(entryId: Long) {
        dao.getReview(entryId)?.let { review ->
            dao.upsertReview(review.copy(
                lastAttemptStatus = ReviewAttemptStatus.NONE.name,
                lastErrorKind = "",
                lastErrorMessage = "",
            ))
        }
    }

    suspend fun saveJourneyDirection(title: String, nowEpochMillis: Long) {
        val normalizedTitle = title.trim().take(MAX_DIRECTION_LENGTH)
        if (normalizedTitle.isEmpty()) {
            dao.clearJourneyDirection()
        } else {
            dao.upsertJourneyDirection(
                JourneyDirectionEntity(
                    title = normalizedTitle,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
            )
        }
    }

    companion object {
        const val MAX_DAY_LENGTH = 5000
        const val MAX_DIRECTION_LENGTH = 80
    }
}

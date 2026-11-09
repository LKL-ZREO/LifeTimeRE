package com.lifetimere.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeTimeDao {
    @Query("SELECT * FROM action_followups ORDER BY dueDate, id")
    fun observeActionFollowups(): Flow<List<ActionFollowupEntity>>

    @Query("SELECT * FROM action_followups WHERE id = :id")
    suspend fun getActionFollowup(id: Long): ActionFollowupEntity?

    @Insert suspend fun insertActionFollowup(followup: ActionFollowupEntity): Long

    @Query("UPDATE action_followups SET dueDate = :date, status = 'PENDING', outcome = '', completedAt = NULL WHERE id = :id")
    suspend fun rescheduleActionFollowup(id: Long, date: String)

    @Query("UPDATE action_followups SET status = :status, outcome = :outcome, completedAt = :completedAt WHERE id = :id")
    suspend fun completeActionFollowup(id: Long, status: String, outcome: String, completedAt: Long)

    @Query("SELECT * FROM projects ORDER BY createdAt DESC, id DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM project_milestones ORDER BY id")
    fun observeProjectMilestones(): Flow<List<ProjectMilestoneEntity>>

    @Query("SELECT * FROM project_entries")
    fun observeProjectEntries(): Flow<List<ProjectEntryEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Query("SELECT * FROM project_milestones WHERE id = :id")
    suspend fun getProjectMilestone(id: Long): ProjectMilestoneEntity?

    @Insert suspend fun insertProject(project: ProjectEntity): Long
    @Insert suspend fun insertProjectMilestone(milestone: ProjectMilestoneEntity): Long
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun linkProjectEntry(link: ProjectEntryEntity)

    @Query("DELETE FROM project_entries WHERE projectId = :projectId AND entryId = :entryId")
    suspend fun unlinkProjectEntry(projectId: Long, entryId: Long)

    @Query("UPDATE projects SET nextStep = :nextStep WHERE id = :id")
    suspend fun updateProjectNextStep(id: Long, nextStep: String)

    @Query("UPDATE project_milestones SET completedAt = :completedAt WHERE id = :id")
    suspend fun updateMilestoneCompletion(id: Long, completedAt: Long?)

    @Query("SELECT * FROM journal_notes ORDER BY createdAt, id")
    fun observeNotes(): Flow<List<JournalNoteEntity>>

    @Query("SELECT * FROM journal_notes WHERE entryId = :entryId ORDER BY createdAt, id")
    suspend fun getNotes(entryId: Long): List<JournalNoteEntity>

    @Insert
    suspend fun insertNote(note: JournalNoteEntity): Long

    @Update
    suspend fun updateNote(note: JournalNoteEntity)

    @Query("DELETE FROM journal_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM journal_notes WHERE entryId = :entryId")
    suspend fun deleteNotes(entryId: Long)

    @Query("SELECT * FROM daily_entries ORDER BY local_date")
    suspend fun getEntries(): List<DailyEntryEntity>

    @Query("SELECT * FROM daily_entries WHERE local_date = :localDate LIMIT 1")
    fun observeEntry(localDate: String): Flow<DailyEntryEntity?>

    @Query("SELECT * FROM daily_entries ORDER BY local_date DESC")
    fun observeEntries(): Flow<List<DailyEntryEntity>>

    @Query("SELECT * FROM daily_entries WHERE local_date = :localDate LIMIT 1")
    suspend fun getEntry(localDate: String): DailyEntryEntity?

    @Query("SELECT * FROM daily_entries WHERE id = :id LIMIT 1")
    suspend fun getEntry(id: Long): DailyEntryEntity?

    @Insert
    suspend fun insertEntry(entry: DailyEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: DailyEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: DailyEntryEntity)

    @Query("SELECT * FROM reviews WHERE entry_id = :entryId LIMIT 1")
    fun observeReview(entryId: Long): Flow<ReviewEntity?>

    @Query("SELECT * FROM reviews")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews")
    suspend fun getReviews(): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE entry_id = :entryId LIMIT 1")
    suspend fun getReview(entryId: Long): ReviewEntity?

    @Upsert
    suspend fun upsertReview(review: ReviewEntity)

    @Insert
    suspend fun insertReviewFeedback(feedback: ReviewFeedbackEntity): Long

    @Query("SELECT * FROM review_feedback WHERE entry_id = :entryId ORDER BY created_at_epoch_millis")
    suspend fun getReviewFeedback(entryId: Long): List<ReviewFeedbackEntity>

    @Query("DELETE FROM evidence WHERE entry_id = :entryId")
    suspend fun deleteEvidence(entryId: Long)

    @Query("DELETE FROM growth_ledger WHERE entry_id = :entryId")
    suspend fun deleteGrowth(entryId: Long)

    @Insert
    suspend fun insertEvidence(evidence: List<EvidenceEntity>)

    @Insert
    suspend fun insertGrowth(growth: List<GrowthLedgerEntity>)

    @Query("SELECT * FROM evidence WHERE entry_id = :entryId ORDER BY action_id")
    fun observeEvidence(entryId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM growth_ledger WHERE entry_id = :entryId ORDER BY action_id")
    fun observeGrowth(entryId: Long): Flow<List<GrowthLedgerEntity>>

    @Query("SELECT growth_ledger.* FROM growth_ledger INNER JOIN reviews ON reviews.entry_id = growth_ledger.entry_id INNER JOIN daily_entries ON daily_entries.id = reviews.entry_id WHERE reviews.status = 'COMPLETE' AND reviews.revision = daily_entries.revision AND growth_ledger.revision = reviews.revision")
    fun observeEffectiveGrowth(): Flow<List<GrowthLedgerEntity>>

    @Query("SELECT daily_entries.id AS entry_id, daily_entries.local_date, daily_entries.time_zone_id, daily_entries.body, daily_entries.created_at_epoch_millis, daily_entries.updated_at_epoch_millis, daily_entries.revision, reviews.summary AS review_summary, reviews.rule_version, reviews.analysis_model, reviews.prompt_version, reviews.schema_version, reviews.analyzed_at_epoch_millis, evidence.action_id, evidence.attribute_id, evidence.intensity, evidence.quote, evidence.explanation, growth_ledger.points FROM daily_entries INNER JOIN reviews ON reviews.entry_id = daily_entries.id LEFT JOIN evidence ON evidence.entry_id = daily_entries.id AND evidence.revision = daily_entries.revision LEFT JOIN growth_ledger ON growth_ledger.entry_id = evidence.entry_id AND growth_ledger.action_id = evidence.action_id AND growth_ledger.revision = daily_entries.revision WHERE reviews.status = 'COMPLETE' AND reviews.revision = daily_entries.revision ORDER BY daily_entries.local_date DESC, evidence.action_id")
    fun observeEffectiveGrowthSources(): Flow<List<GrowthSourceRow>>

    @Query("""
        SELECT COALESCE(SUM(g.points), 0) FROM growth_ledger g
        JOIN reviews r ON r.entry_id = g.entry_id AND r.revision = g.revision
        JOIN daily_entries e ON e.id = r.entry_id AND e.revision = r.revision
        WHERE r.status = 'COMPLETE' AND g.attribute_id IN (:attributeIds)
    """)
    suspend fun effectiveGrowthPoints(attributeIds: List<String>): Long

    @Query("SELECT * FROM growth_rewards ORDER BY rewardId")
    fun observeGrowthRewards(): Flow<List<GrowthRewardEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertGrowthReward(reward: GrowthRewardEntity)

    @Query("SELECT * FROM keepsakes ORDER BY id DESC")
    fun observeKeepsakes(): Flow<List<KeepsakeEntity>>

    @Query("SELECT * FROM keepsakes WHERE id = :id")
    suspend fun getKeepsake(id: Long): KeepsakeEntity?

    @Insert
    suspend fun insertKeepsake(keepsake: KeepsakeEntity)

    @Query("UPDATE keepsakes SET shelfSlot = NULL WHERE shelfSlot = 1")
    suspend fun clearShelf()

    @Query("UPDATE keepsakes SET chapterSlot = NULL WHERE chapterSlot = 1")
    suspend fun clearChapterDisplay()

    @Query("UPDATE keepsakes SET shelfSlot = 1 WHERE id = :id")
    suspend fun displayOnShelf(id: Long)

    @Query("UPDATE keepsakes SET chapterSlot = 1 WHERE id = :id")
    suspend fun displayInChapter(id: Long)


    @Query("SELECT * FROM journey_direction WHERE id = 1 LIMIT 1")
    fun observeJourneyDirection(): Flow<JourneyDirectionEntity?>

    @Upsert
    suspend fun upsertJourneyDirection(direction: JourneyDirectionEntity)

    @Query("DELETE FROM journey_direction")
    suspend fun clearJourneyDirection()

    @Query("SELECT * FROM traveler_profiles WHERE id = 1 LIMIT 1")
    fun observeTravelerProfile(): Flow<TravelerProfileEntity?>

    @Query("SELECT * FROM traveler_profiles WHERE id = 1 LIMIT 1")
    suspend fun getTravelerProfile(): TravelerProfileEntity?

    @Query("SELECT * FROM traveler_answers WHERE profileId = 1")
    fun observeTravelerAnswers(): Flow<List<TravelerAnswerEntity>>

    @Query("SELECT * FROM traveler_answers WHERE profileId = 1")
    suspend fun getTravelerAnswers(): List<TravelerAnswerEntity>

    @Upsert
    suspend fun upsertTravelerProfile(profile: TravelerProfileEntity)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertTravelerAnswer(answer: TravelerAnswerEntity)

    @Query("DELETE FROM traveler_answers WHERE profileId = 1")
    suspend fun clearTravelerAnswers()
}

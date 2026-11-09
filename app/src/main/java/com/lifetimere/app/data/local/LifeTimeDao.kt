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

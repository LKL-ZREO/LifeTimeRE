package com.lifetimere.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifetimere.app.data.local.LifeTimeDatabase
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.CampStage
import com.lifetimere.app.domain.CampMilestones
import com.lifetimere.app.domain.DetectedAction
import com.lifetimere.app.domain.DetectedReview
import com.lifetimere.app.domain.ReviewAnalysisMetadata
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.domain.TravelerQuestionnaire
import com.lifetimere.app.data.model.TravelerProfileStatus
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeTimeRepositoryTest {
    private lateinit var database: LifeTimeDatabase
    private lateinit var repository: LifeTimeRepository
    private val date = LocalDate.of(2026, 11, 1)

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, LifeTimeDatabase::class.java).build()
        repository = LifeTimeRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun concurrentClaimAndRepeatSubmissionAwardOnce() = runBlocking {
        val entry = repository.saveEntry(date, "Asia/Shanghai", "晨跑了三公里。", 1)
        val claims = coroutineScope {
            listOf(async { repository.beginReview(entry.id, 1) }, async { repository.beginReview(entry.id, 1) })
                .map { it.await() }
        }
        assertEquals(1, claims.count { it != null })
        val result = review("晨跑了三公里。")
        assertTrue(repository.completeReview(entry.id, 1, result))
        assertNull(repository.beginReview(entry.id, 1, retry = true))
        assertFalse(repository.completeReview(entry.id, 1, result))
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
    }

    @Test fun revisionReplacesOldGrowthAndRejectsLateResult() = runBlocking {
        val first = repository.saveEntry(date, "Asia/Shanghai", "晨跑了三公里。", 1)
        assertNotNull(repository.beginReview(first.id, first.revision))
        assertTrue(repository.completeReview(first.id, first.revision, review(first.body)))
        val revised = repository.updateEntry(first.id, "今天读书。", 2)!!
        assertEquals(2, revised.revision)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
        assertFalse(repository.completeReview(first.id, 1, review(first.body)))
        assertNotNull(repository.beginReview(first.id, 2))
        assertTrue(repository.completeReview(first.id, 2, DetectedReview("记录了阅读。", listOf(
            DetectedAction("learning", CharacterAttribute.KNOWLEDGE, 2, "读书", "阅读"),
        ))))
        assertEquals(mapOf(CharacterAttribute.KNOWLEDGE to 2), repository.observeAttributeTotals().first())
        assertEquals(ReviewStatus.COMPLETE, repository.observeReview(first.id).first()?.status)
        repository.deleteEntry(first.id)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
        assertNull(repository.observeReview(first.id).first())
    }

    @Test fun zeroGrowthAndFailureRetryAreValid() = runBlocking {
        val entry = repository.saveEntry(date, "Asia/Shanghai", "今天休息。", 1)
        assertNotNull(repository.beginReview(entry.id, 1))
        repository.failReview(entry.id, 1)
        assertEquals(ReviewStatus.FAILED, repository.observeReview(entry.id).first()?.status)
        assertNotNull(repository.beginReview(entry.id, 1, retry = true))
        assertTrue(repository.completeReview(entry.id, 1, DetectedReview("今天休息。", emptyList())))
        assertEquals(ReviewStatus.COMPLETE, repository.observeReview(entry.id).first()?.status)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
    }

    @Test fun feedbackIsAuditedAndValidatedReplacementUpdatesProvenance() = runBlocking {
        val entry = repository.saveEntry(date, "Asia/Shanghai", "晨跑了三公里。", 1)
        assertNotNull(repository.beginReview(entry.id, entry.revision))
        val first = review(entry.body).copy(
            metadata = ReviewAnalysisMetadata("model-a", "prompt-v1", "schema-v1", 100),
        )
        assertTrue(repository.completeReview(entry.id, entry.revision, first))

        val feedbackEntry = repository.recordReviewFeedback(
            entry.id,
            ReviewFeedbackReason.ATTRIBUTE_MISMATCH,
            200,
        )
        assertNotNull(feedbackEntry)
        assertEquals(1, database.dao().getReviewFeedback(entry.id).size)
        assertEquals("prompt-v1", database.dao().getReviewFeedback(entry.id).single().promptVersion)

        val replacement = first.copy(
            summary = "重新分析完成。",
            metadata = ReviewAnalysisMetadata("model-b", "prompt-v2", "schema-v1", 300),
        )
        assertTrue(repository.replaceCompleteReview(entry.id, entry.revision, replacement))
        val saved = repository.observeReview(entry.id).first()!!
        assertEquals("重新分析完成。", saved.summary)
        assertEquals("model-b", saved.metadata.model)
        assertEquals("prompt-v2", saved.metadata.promptVersion)
        assertEquals(300, saved.metadata.analyzedAtEpochMillis)
        assertEquals(1, saved.ruleVersion)
    }

    @Test fun campIsDerivedFromDistinctEffectiveReviewDates() = runBlocking {
        assertEquals(CampStage.DORMANT, repository.observeCampState().first().stage)

        val first = settleZero(date, "第一天休息。", 1)
        val firstStage = repository.observeCampState().first()
        assertEquals(CampStage.FIRST_ECHO, firstStage.stage)
        assertEquals(1, firstStage.settledDates)
        assertTrue(firstStage.sources.single().evidence.isEmpty())

        val second = settleZero(date.plusDays(1), "第二天整理房间。", 2)
        assertEquals(CampStage.FIRST_ECHO, repository.observeCampState().first().stage)
        val third = settleZero(date.plusDays(2), "第三天散步。", 3)
        assertEquals(CampStage.AWAKENED, repository.observeCampState().first().stage)
        assertEquals(3, repository.observeCampState().first().settledDates)

        repository.deleteEntry(third.id)
        assertEquals(CampStage.FIRST_ECHO, repository.observeCampState().first().stage)
        repository.updateEntry(first.id, "第一天修改后等待回响。", 4)
        assertEquals(1, repository.observeCampState().first().settledDates)
        repository.deleteEntry(second.id)
        assertEquals(CampStage.DORMANT, repository.observeCampState().first().stage)
    }

    @Test fun campMilestonesFollowCumulativeReviewDates() = runBlocking {
        assertEquals(1, CampMilestones.nextTarget(0))
        assertEquals(2, CampMilestones.nextTarget(1))
        assertEquals(3, CampMilestones.nextTarget(2))
        assertEquals(5, CampMilestones.nextTarget(3))
        assertEquals(7, CampMilestones.nextTarget(5))
        assertEquals(14, CampMilestones.nextTarget(7))

        repeat(7) { offset ->
            settleZero(date.plusDays(offset.toLong()), "第 ${offset + 1} 天完成回顾。", offset + 10L)
            val state = repository.observeCampState().first()
            assertEquals(offset + 1, state.settledDates)
            if (offset + 1 < 7) assertEquals(CampMilestones.targets.lastOrNull { it <= offset + 1 } ?: 0, state.reachedMilestone)
        }
        assertEquals(14, repository.observeCampState().first().nextMilestone)
    }

    @Test fun travelerProfileCanBeCompletedAndRetakenWithoutChangingGrowth() = runBlocking {
        repository.beginTravelerRegistration(nowEpochMillis = 1)
        TravelerQuestionnaire.questions.forEachIndexed { index, question ->
            repository.saveTravelerAnswer(question.id, question.options.first().id, index + 1, index + 2L)
        }

        assertTrue(repository.completeTravelerRegistration(20))
        val completed = repository.observeTravelerProfile().first()!!
        assertEquals(TravelerProfileStatus.COMPLETED, completed.status)
        assertEquals(TravelerQuestionnaire.QUESTION_COUNT, completed.answers.size)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
        assertEquals(0, repository.observeCampState().first().settledDates)

        repository.beginTravelerRegistration(retake = true, nowEpochMillis = 30)
        repository.skipTravelerRegistration(31)
        val restored = repository.observeTravelerProfile().first()!!
        assertEquals(TravelerProfileStatus.COMPLETED, restored.status)
        assertEquals(completed.tendencies, restored.tendencies)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
    }

    private fun review(body: String) = DetectedReview("记录了运动。", listOf(
        DetectedAction("exercise", CharacterAttribute.STRENGTH, 1, body, "身体活动"),
    ))

    private suspend fun settleZero(date: LocalDate, body: String, now: Long) =
        repository.saveEntry(date, "Asia/Shanghai", body, now).also { entry ->
            assertNotNull(repository.beginReview(entry.id, entry.revision))
            assertTrue(repository.completeReview(entry.id, entry.revision, DetectedReview("完成回顾。", emptyList())))
        }
}

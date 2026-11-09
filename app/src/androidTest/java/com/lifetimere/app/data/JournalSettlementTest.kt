package com.lifetimere.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifetimere.app.data.local.LifeTimeDatabase
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.data.model.ReviewAttemptStatus
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.GrowthReward
import com.lifetimere.app.domain.DailySettlement
import com.lifetimere.app.domain.DetectedAction
import com.lifetimere.app.domain.DetectedReview
import com.lifetimere.app.domain.ReviewService
import com.lifetimere.app.domain.SettlementOutcome
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalSettlementTest {
    private lateinit var database: LifeTimeDatabase
    private lateinit var repository: LifeTimeRepository
    private val date = LocalDate.of(2026, 11, 3)
    private val result = DetectedReview("完成运动。", listOf(DetectedAction("run", CharacterAttribute.STRENGTH, 1, "跑了三公里", "完成运动")))

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, LifeTimeDatabase::class.java).build()
        repository = LifeTimeRepository(database)
    }
    @After fun close() = database.close()

    @Test fun multipleCapturesPersistWithoutAiAndSettleAsOneDay() = runBlocking {
        val first = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        repository.appendNote(date, "Asia/Shanghai", "看了半小时书", 2)
        assertEquals(2, repository.observeNotes().first().size)
        assertEquals("跑了三公里\n\n看了半小时书", repository.observeJournalEntries().first().single().body)
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
        val snapshot = repository.journalSnapshot(first.id)!!
        assertTrue(repository.settleJournal(snapshot, result))
        assertFalse(repository.settleJournal(snapshot, result))
        assertNull(repository.journalSnapshot(first.id))
        assertEquals(1, repository.observeCampState().first().settledDates)
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
    }

    @Test fun growthRewardsUnlockAtThresholdAndSurviveRecalculationAndDeletion() = runBlocking {
        val first = repository.appendNote(date, "Asia/Shanghai", "画完了一张插画", 1)
        val firstResult = DetectedReview("完成插画。", listOf(
            DetectedAction("draw", CharacterAttribute.CREATION, 3, "画完了一张插画", "完成创作"),
        ))
        val snapshot = repository.journalSnapshot(first.id)!!
        assertTrue(repository.settleJournal(snapshot, firstResult))
        assertFalse(repository.settleJournal(snapshot, firstResult))
        assertEquals(GrowthReward.SKETCH, repository.observeGrowthRewards().first().single().reward)

        val second = repository.appendNote(date.plusDays(1), "Asia/Shanghai", "修好了一把椅子", 2)
        val secondResult = DetectedReview("修好椅子。", listOf(
            DetectedAction("repair", CharacterAttribute.CREATION, 2, "修好了一把椅子", "完成修缮"),
        ))
        assertTrue(repository.settleJournal(repository.journalSnapshot(second.id)!!, secondResult))
        val unlocks = repository.observeGrowthRewards().first().associateBy { it.reward }
        assertEquals(first.id, unlocks.getValue(GrowthReward.SKETCH).sourceEntryId)
        assertEquals(second.id, unlocks.getValue(GrowthReward.TOOL_BOARD).sourceEntryId)

        val note = repository.observeNotes().first().single { it.entryId == second.id }
        repository.editNote(second.id, note.id, "我想修椅子", 3)
        assertTrue(repository.settleJournal(repository.journalSnapshot(second.id)!!, DetectedReview("记录了想法。", emptyList())))
        assertEquals(3, repository.observeAttributeTotals().first()[CharacterAttribute.CREATION])
        assertEquals(2, repository.observeGrowthRewards().first().size)

        repository.deleteEntry(first.id)
        assertNull(repository.observeGrowthRewards().first().single { it.reward == GrowthReward.SKETCH }.sourceEntryId)
        assertEquals(2, repository.observeGrowthRewards().first().size)
    }

    @Test fun actionFollowupCanBeRescheduledAndCompletedOnce() = runBlocking {
        val projectId = repository.createProject("论文")!!
        assertTrue(repository.createActionFollowup(projectId, "修改第一节", date.plusDays(1)))
        val followup = repository.observeActionFollowups().first().single()
        assertTrue(repository.rescheduleActionFollowup(followup.id, date.plusDays(2)))
        assertEquals(date.plusDays(2).toString(), repository.observeActionFollowups().first().single().dueDate)
        assertTrue(repository.completeActionFollowup(followup.id, "ADJUSTED", "改为先核对数据"))
        assertFalse(repository.completeActionFollowup(followup.id, "DONE", "重复完成"))
        assertFalse(repository.rescheduleActionFollowup(followup.id, date.plusDays(3)))
        assertEquals("改为先核对数据", repository.observeActionFollowups().first().single().outcome)
    }

    @Test fun projectTracksMilestonesAndRemovesDeletedJournalLinks() = runBlocking {
        val projectId = repository.createProject("写论文")!!
        val day = repository.appendNote(date, "Asia/Shanghai", "完成提纲", 1)
        assertTrue(repository.setProjectEntry(projectId, day.id, true))
        assertTrue(repository.setProjectNextStep(projectId, "修改第一节"))
        assertTrue(repository.addProjectMilestone(projectId, "完成初稿"))
        val milestone = repository.observeProjectMilestones().first().single()
        assertTrue(repository.setMilestoneComplete(milestone.id, true))
        assertNotNull(repository.observeProjectMilestones().first().single().completedAt)
        assertEquals("修改第一节", repository.observeProjects().first().single().nextStep)
        repository.deleteEntry(day.id)
        assertTrue(repository.observeProjectEntries().first().isEmpty())
        assertEquals(projectId, repository.observeProjects().first().single().id)
    }

    @Test fun keepsakeRequiresSettledOriginalAndDisplaysIndependentlyUntilSourceChanges() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        assertFalse(repository.craftKeepsake(day.id, "跑了三公里", "晨跑", "木牌"))
        assertTrue(repository.settleJournal(repository.journalSnapshot(day.id)!!, result))
        assertFalse(repository.craftKeepsake(day.id, "跑了五公里", "晨跑", "木牌"))
        assertTrue(repository.craftKeepsake(day.id, "跑了三公里", "晨跑", "木牌"))
        val item = repository.observeKeepsakes().first().single()
        assertTrue(repository.displayKeepsake(item.id, chapter = false))
        assertTrue(repository.displayKeepsake(item.id, chapter = true))
        val displayed = repository.observeKeepsakes().first().single()
        assertNotNull(displayed.shelfSlot)
        assertNotNull(displayed.chapterSlot)
        assertTrue(repository.displayKeepsake(null, chapter = true))
        assertNotNull(repository.observeKeepsakes().first().single().shelfSlot)

        val note = repository.observeNotes().first().single()
        repository.editNote(day.id, note.id, "今天休息", 2)
        assertFalse(repository.displayKeepsake(item.id, chapter = false))
        repository.deleteEntry(day.id)
        assertTrue(repository.observeKeepsakes().first().isEmpty())
    }

    @Test fun lateCaptureRetainsPointsAndNextSettlementReplacesInsteadOfAdding() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        repository.settleJournal(repository.journalSnapshot(day.id)!!, result)
        repository.appendNote(date, "Asia/Shanghai", "读完一章", 2)
        assertEquals(ReviewStatus.PENDING, repository.observeJournalStatuses().first()[day.id])
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
        val updated = result.copy(actions = result.actions + DetectedAction("read", CharacterAttribute.KNOWLEDGE, 1, "读完一章", "阅读"))
        assertTrue(repository.settleJournal(repository.journalSnapshot(day.id)!!, updated))
        assertEquals(mapOf(CharacterAttribute.STRENGTH to 1, CharacterAttribute.KNOWLEDGE to 1), repository.observeAttributeTotals().first())
        assertEquals(1, repository.observeCampState().first().settledDates)
    }

    @Test fun appendWhileAiRunsRejectsStaleResponseAndNextPassIncludesEverything() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val service = object : ReviewService {
            override suspend fun analyze(entry: com.lifetimere.app.data.model.DailyEntry): DetectedReview {
                calls++
                started.complete(Unit)
                finish.await()
                return result
            }
        }
        val settlement = DailySettlement(repository) { service }
        val running = async { settlement.settle(day.id) }
        started.await()
        repository.appendNote(date, "Asia/Shanghai", "又读了一章书", 2)
        finish.complete(Unit)
        assertEquals(SettlementOutcome.STALE, running.await())
        assertTrue(repository.observeAttributeTotals().first().isEmpty())
        assertEquals(SettlementOutcome.COMPLETED, settlement.settle(day.id))
        assertEquals(SettlementOutcome.ALREADY_COMPLETE, settlement.settle(day.id))
        assertEquals(2, calls)
        assertTrue(repository.observeEntry(date).first()!!.body.contains("又读了一章书"))
    }

    @Test fun failedAnalysisPreservesPreviousSnapshotAndCanRetry() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        repository.settleJournal(repository.journalSnapshot(day.id)!!, result)
        repository.appendNote(date, "Asia/Shanghai", "今天休息", 2)
        val failing = DailySettlement(repository) { object : ReviewService {
            override suspend fun analyze(entry: com.lifetimere.app.data.model.DailyEntry): DetectedReview = error("offline")
        } }
        try { failing.settle(day.id); fail("Expected offline failure") } catch (_: IllegalStateException) { }
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
        val failedReview = repository.observeReview(day.id).first()!!
        assertEquals(ReviewStatus.COMPLETE, failedReview.status)
        assertEquals(ReviewAttemptStatus.FAILED, failedReview.attemptStatus)
        assertTrue(failedReview.failureMessage.isNotBlank())
        assertEquals(ReviewStatus.FAILED, repository.observeJournalStatuses().first()[day.id])
        assertNotNull(repository.journalSnapshot(day.id))
        assertTrue(failing.active.value.isEmpty())
    }

    @Test fun concurrentSettlementRequestsOnlyCallAiOnce() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val settlement = DailySettlement(repository) { object : ReviewService {
            override suspend fun analyze(entry: com.lifetimere.app.data.model.DailyEntry): DetectedReview {
                calls++
                started.complete(Unit)
                finish.await()
                return result
            }
        } }
        val first = async { settlement.settle(day.id) }
        started.await()
        val second = async { settlement.settle(day.id) }
        finish.complete(Unit)
        assertEquals(SettlementOutcome.COMPLETED, first.await())
        assertEquals(SettlementOutcome.ALREADY_COMPLETE, second.await())
        assertEquals(1, calls)
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
    }

    @Test fun editsDeletionAndWholeDayCorrectionInvalidateInFlightResults() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        repository.appendNote(date, "Asia/Shanghai", "读完一章", 2)
        val stale = repository.journalSnapshot(day.id)!!
        val note = repository.observeNotes().first().first()
        repository.editNote(day.id, note.id, "计划跑三公里", 3)
        assertFalse(repository.settleJournal(stale, result))
        val next = repository.journalSnapshot(day.id)!!
        repository.removeNote(day.id, note.id)
        assertFalse(repository.settleJournal(next, result))
        repository.replaceJournal(day.id, "今天休息", 4)
        assertEquals("今天休息", repository.journalSnapshot(day.id)!!.entry.body)
        repository.settleJournal(repository.journalSnapshot(day.id)!!, DetectedReview("今天休息。", emptyList()))
        repository.removeNote(day.id, repository.observeNotes().first().single().id)
        assertTrue(repository.observeJournalEntries().first().isEmpty())
        assertTrue(repository.observeGrowthSources().first().isEmpty())
    }

    @Test fun lengthLimitRollsBackAndNextDayIsIndependent() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        try { repository.appendNote(date, "Asia/Shanghai", "字".repeat(5000), 2); fail("Expected limit") } catch (_: IllegalArgumentException) { }
        assertEquals(1, repository.observeNotes().first().size)
        repository.appendNote(date.plusDays(1), "Asia/Shanghai", "读完一章", 3)
        assertEquals(2, repository.observeJournalEntries().first().size)
        val snapshot = repository.journalSnapshot(day.id)!!
        repository.deleteEntry(day.id)
        assertFalse(repository.settleJournal(snapshot, result))
        assertEquals(1, repository.observeNotes().first().size)
    }

    @Test fun invalidReplacementRollsBackWithoutLosingPreviousGrowth() = runBlocking {
        val day = repository.appendNote(date, "Asia/Shanghai", "跑了三公里", 1)
        repository.settleJournal(repository.journalSnapshot(day.id)!!, result)
        repository.replaceJournal(day.id, "今天休息", 2)
        try { repository.settleJournal(repository.journalSnapshot(day.id)!!, result); fail("Bad evidence") } catch (_: IllegalArgumentException) { }
        assertEquals("跑了三公里", repository.observeEntry(date).first()!!.body)
        assertEquals(1, repository.observeAttributeTotals().first()[CharacterAttribute.STRENGTH])
    }
}

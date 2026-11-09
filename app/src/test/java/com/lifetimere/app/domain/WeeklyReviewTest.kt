package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.ReviewEvidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReviewTest {
    @Test fun keepsExactQuotesAndExcludesStaleOrOtherWeekSources() {
        val today = LocalDate.of(2026, 11, 5)
        val current = entry(1, LocalDate.of(2026, 11, 3), "画完了第一张场景草图")
        val pendingEdit = entry(2, LocalDate.of(2026, 11, 4), "改成今天休息")
        val previousWeek = entry(3, LocalDate.of(2026, 10, 30), "读完一章")
        val review = WeeklyReviewBuilder.build(
            today,
            listOf(current, pendingEdit, previousWeek),
            listOf(
                source(current, "画完了第一张场景草图", 2),
                source(pendingEdit.copy(body = "跑了三公里"), "跑了三公里", 1),
                source(previousWeek, "读完一章", 1),
            ),
        )

        assertEquals(LocalDate.of(2026, 11, 2), review.startDate)
        assertEquals(2, review.recordedDays)
        assertEquals(1, review.reviewedDays)
        assertEquals(1, review.pendingDays)
        assertEquals(2, review.totalGrowth)
        assertEquals(listOf("画完了第一张场景草图"), review.items.single().quotes)
        assertTrue(review.items.single().fullBody.contains(review.items.single().quotes.single()))
    }

    private fun entry(id: Long, date: LocalDate, body: String) =
        DailyEntry(id, date, "Asia/Shanghai", body, 1, 1, 1)

    private fun source(entry: DailyEntry, quote: String, points: Int) = GrowthSource(
        entry = entry,
        evidence = listOf(ReviewEvidence("a$points", CharacterAttribute.SKILL, 1, quote, "完成了实际产出", points)),
        summary = "这一天完成了一项真实行动。",
    )
}

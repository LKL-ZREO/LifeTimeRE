package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthRulesTest {
    private val body = "跑步。健身。阅读。学习。编程。创作。坚持。求助。"

    @Test fun capsEachAttributeAndWholeDay() {
        val actions = listOf(
            action("a", CharacterAttribute.STRENGTH, 3, "跑步"),
            action("b", CharacterAttribute.STRENGTH, 3, "健身"),
            action("c", CharacterAttribute.KNOWLEDGE, 3, "阅读"),
            action("d", CharacterAttribute.SKILL, 3, "编程"),
            action("e", CharacterAttribute.RESILIENCE, 3, "求助"),
        )
        val awards = GrowthRules.settle(body, DetectedReview("", actions))
        assertEquals(8, awards.sumOf { it.points })
        assertEquals(3, awards.filter { it.action.attribute == CharacterAttribute.STRENGTH }.sumOf { it.points })
        assertEquals(listOf(3, 3, 2), awards.map { it.points })
    }

    @Test fun rejectsDuplicateAndInventedEvidence() {
        val action = action("a", CharacterAttribute.STRENGTH, 1, "跑步")
        assertThrows(IllegalArgumentException::class.java) {
            GrowthRules.settle(body, DetectedReview("", listOf(action, action)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GrowthRules.settle(body, DetectedReview("", listOf(action.copy(quote = "游泳"))))
        }
    }

    @Test fun demoReturnsQuotedActionsOrNeutralZero() = runBlocking {
        val service = DemoReviewService()
        val active = service.analyze(entry("晨跑了三公里；晚上读了半小时书。"))
        assertTrue(active.actions.isNotEmpty())
        assertTrue(active.actions.all { "晨跑了三公里；晚上读了半小时书。".contains(it.quote) })
        assertTrue(GrowthRules.settle("晨跑了三公里；晚上读了半小时书。", active).isNotEmpty())
        val quiet = service.analyze(entry("今天休息，感觉平静。"))
        assertTrue(GrowthRules.settle("今天休息，感觉平静。", quiet).isEmpty())
        val notDone = service.analyze(entry("今天没跑步，也只是计划学习。"))
        assertTrue(notDone.actions.isEmpty())
    }

    private fun action(id: String, attribute: CharacterAttribute, intensity: Int, quote: String) =
        DetectedAction(id, attribute, intensity, quote, "test")

    private fun entry(text: String) = DailyEntry(1, LocalDate.of(2026, 11, 1), "Asia/Shanghai", text, 1, 1, 1)
}

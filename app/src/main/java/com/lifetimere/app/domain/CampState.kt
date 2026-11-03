package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.ReviewEvidence

enum class CampStage(val label: String) {
    DORMANT("初始营地"),
    FIRST_ECHO("第一簇回响"),
    AWAKENED("营地苏醒"),
}

data class GrowthSource(
    val entry: DailyEntry,
    val evidence: List<ReviewEvidence>,
    val summary: String = "",
    val ruleVersion: Int = GrowthRules.VERSION,
    val metadata: ReviewAnalysisMetadata = ReviewAnalysisMetadata(),
)

data class CampState(
    val stage: CampStage = CampStage.DORMANT,
    val settledDates: Int = 0,
    val sources: List<GrowthSource> = emptyList(),
) {
    val reachedMilestone: Int
        get() = if (settledDates >= 7) {
            (settledDates / 7) * 7
        } else {
            CampMilestones.targets.lastOrNull { settledDates >= it } ?: 0
        }

    val nextMilestone: Int
        get() = CampMilestones.nextTarget(settledDates)

    val milestoneName: String
        get() = CampMilestones.nameFor(reachedMilestone)
}

object CampMilestones {
    val targets = listOf(1, 2, 3, 5, 7)

    fun nextTarget(settledDates: Int): Int =
        targets.firstOrNull { settledDates < it } ?: ((settledDates / 7) + 1) * 7

    fun nameFor(target: Int): String = when (target) {
        0 -> "尚未安营"
        1 -> "篝火初燃"
        2 -> "营地修整"
        3 -> "工坊苏醒"
        5 -> "小径延伸"
        7 -> "首周旅程"
        else -> "新的旅程章节"
    }
}

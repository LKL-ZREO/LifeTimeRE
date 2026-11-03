package com.lifetimere.app.domain

import com.lifetimere.app.data.model.DailyEntry

/** Fixed local examples for validating the flow, not an AI interpretation. */
class DemoReviewService : ReviewService {
    private data class Pattern(
        val id: String,
        val attribute: CharacterAttribute,
        val regex: Regex,
        val explanation: String,
    )

    private val patterns = listOf(
        Pattern("exercise", CharacterAttribute.STRENGTH, Regex("跑步|晨跑|健身|力量训练|俯卧撑|举重|锻炼|运动"), "记录了身体活动"),
        Pattern("persistence", CharacterAttribute.ENDURANCE, Regex("坚持|持续|专注|练习了|训练了|完成了.*公里"), "记录了持续投入"),
        Pattern("learning", CharacterAttribute.KNOWLEDGE, Regex("阅读|读书|读了|学习|复习|课程|上课|研究"), "记录了学习或探索"),
        Pattern("making", CharacterAttribute.SKILL, Regex("创作|制作|写作|画画|绘画|编程|做了.*项目|完成了.*作品"), "记录了创作或实践"),
        Pattern("resilience", CharacterAttribute.RESILIENCE, Regex("克服|调整计划|重新开始|寻求帮助|求助|面对困难|解决了.*问题"), "记录了应对困难的行动"),
    )
    private val unfulfilledPrefix = Regex("(?:没有|没|未|不|想|计划|打算|准备).{0,5}$")

    override suspend fun analyze(entry: DailyEntry): DetectedReview {
        val actions = patterns.mapNotNull { pattern ->
            val match = pattern.regex.findAll(entry.body).firstOrNull { candidate ->
                val clauseStart = entry.body.lastIndexOfAny(charArrayOf('。', '；', ';', '\n'), candidate.range.first - 1) + 1
                !unfulfilledPrefix.containsMatchIn(entry.body.substring(clauseStart, candidate.range.first))
            } ?: return@mapNotNull null
            val start = entry.body.lastIndexOfAny(charArrayOf('。', '；', ';', '\n'), match.range.first - 1) + 1
            val end = entry.body.indexOfAny(charArrayOf('。', '；', ';', '\n'), match.range.last + 1)
                .let { if (it < 0) entry.body.length else it }
            val quote = entry.body.substring(start, end).trim().take(120)
            if (!quote.contains(match.value)) return@mapNotNull null
            DetectedAction(pattern.id, pattern.attribute, 1, quote, pattern.explanation)
        }
        val summary = if (actions.isEmpty()) {
            "今天的经历已记录。演示识别器未找到足够明确的行动依据。"
        } else {
            "今天记录了${actions.size}类行动，以下回响来自旅志原文。"
        }
        return DetectedReview(summary, actions)
    }
}

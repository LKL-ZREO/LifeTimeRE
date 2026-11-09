package com.lifetimere.app.domain

import kotlin.math.roundToInt

data class TravelerOption(
    val id: String,
    val label: String,
    val scores: Map<CharacterAttribute, Int>,
)

data class TravelerQuestion(
    val id: String,
    val prompt: String,
    val options: List<TravelerOption>,
)

object TravelerQuestionnaire {
    const val VERSION = 2
    const val QUESTION_COUNT = 6
    const val NEUTRAL_OPTION_ID = "uncertain"

    private fun option(
        id: String,
        label: String,
        strength: Int? = null,
        knowledge: Int? = null,
        creation: Int? = null,
        spirit: Int? = null,
    ) = TravelerOption(
        id = id,
        label = label,
        scores = buildMap {
            strength?.let { put(CharacterAttribute.STRENGTH, it) }
            knowledge?.let { put(CharacterAttribute.KNOWLEDGE, it) }
            creation?.let { put(CharacterAttribute.CREATION, it) }
            spirit?.let { put(CharacterAttribute.SPIRIT, it) }
        },
    )

    val questions = listOf(
        TravelerQuestion(
            id = "free_time",
            prompt = "最近有一段空闲时间时，你更可能做什么？",
            options = listOf(
                option("move", "出去走走，活动一下身体", strength = 4),
                option("explore", "看看感兴趣的新内容", knowledge = 4),
                option("make", "动手做点东西或整理手头项目", creation = 4),
                option("recover", "先歇一会儿，让自己缓过来", spirit = 4),
            ),
        ),
        TravelerQuestion(
            id = "new_topic",
            prompt = "碰到一个不熟悉的问题时，你通常先怎么开始？",
            options = listOf(
                option("read_examples", "找资料或例子，先弄清来龙去脉", knowledge = 4, creation = 3),
                option("try_small", "先试一个小步骤，边做边看", creation = 4, spirit = 3),
                option("ask_someone", "找熟悉的人聊聊，听听不同思路", spirit = 4, knowledge = 3),
                option("break_down", "把问题拆小，再逐项处理", knowledge = 3),
            ),
        ),
        TravelerQuestion(
            id = "ongoing_effort",
            prompt = "需要持续投入的一件事，你更常用哪种方式推进？",
            options = listOf(
                option("routine", "安排固定时间，也留出休息的空档", spirit = 4),
                option("practice", "反复练习，留意每次哪里不一样", creation = 4),
                option("physical_goal", "设一个身体活动的小目标", strength = 4),
                option("learn_step", "边做边补充知识，逐渐调整方法", knowledge = 4, creation = 3),
            ),
        ),
        TravelerQuestion(
            id = "plan_changed",
            prompt = "计划临时被打断时，你通常会怎么应对？",
            options = listOf(
                option("reorder", "重新排一下顺序，找新的切入点", spirit = 4),
                option("smaller_goal", "把目标缩小一点，先完成下一步", spirit = 3),
                option("get_support", "和别人商量，看看能不能一起解决", spirit = 4, knowledge = 3),
                option("take_pause", "先停下来恢复状态，之后再决定", spirit = 4),
            ),
        ),
        TravelerQuestion(
            id = "fulfilling_day",
            prompt = "最近让你觉得充实的一天，更接近哪一种？",
            options = listOf(
                option("active_day", "活动了身体，完成一段挑战", strength = 4),
                option("finished_task", "把一件拖了些时候的事持续做完", spirit = 3),
                option("learned", "学到新东西，理解了一件原本陌生的事", knowledge = 4),
                option("created", "做出了作品、菜肴或一个实用的小成果", creation = 4),
            ),
        ),
        TravelerQuestion(
            id = "difficulty",
            prompt = "遇到一件暂时做不好的事时，你更可能先做什么？",
            options = listOf(
                option("retry", "换个节奏，缓一缓再试", spirit = 4),
                option("change_method", "换个工具或方法试试", creation = 4, spirit = 3),
                option("study", "先找个示例或说明，补足需要的知识", knowledge = 4, creation = 3),
                option("rest_or_help", "休息一下或请人帮忙，再接着处理", spirit = 4),
            ),
        ),
    ).map { question ->
        val relevantAttributes = question.options.flatMap { it.scores.keys }.toSet()
        question.copy(
            options = question.options.map { option ->
                option.copy(scores = relevantAttributes.associateWith { attribute -> option.scores[attribute] ?: 2 })
            } + TravelerOption(
                id = NEUTRAL_OPTION_ID,
                label = "最近没遇到 / 不确定",
                scores = relevantAttributes.associateWith { 2 },
            ),
        )
    }

    fun question(index: Int): TravelerQuestion = questions[index]

    fun findOption(questionId: String, optionId: String): TravelerOption? =
        questions.firstOrNull { it.id == questionId }?.options?.firstOrNull { it.id == optionId }
}

object TravelerProfileScorer {
    fun score(answers: Map<String, String>): Map<CharacterAttribute, Int> {
        require(answers.keys.all { id -> TravelerQuestionnaire.questions.any { it.id == id } }) {
            "Unknown traveler question"
        }
        require(answers.keys.containsAll(TravelerQuestionnaire.questions.map { it.id })) {
            "Traveler questionnaire is incomplete"
        }
        return CharacterAttribute.active.associateWith { attribute ->
            val values = TravelerQuestionnaire.questions.mapNotNull { question ->
                val optionId = answers.getValue(question.id)
                val selected = TravelerQuestionnaire.findOption(question.id, optionId)
                    ?: throw IllegalArgumentException("Unknown traveler option")
                selected.scores[attribute]
            }
            require(values.isNotEmpty()) { "No questions score $attribute" }
            (1 + values.average().roundToInt()).coerceIn(1, 5)
        }
    }
}

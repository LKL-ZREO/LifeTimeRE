package com.lifetimere.app.domain

enum class CharacterAttribute(val label: String, val description: String) {
    STRENGTH("力量", "体力活动与力量练习留下的回响"),
    ENDURANCE("耐力", "运动、专注和持续投入留下的回响"),
    KNOWLEDGE("学识", "阅读、学习和探索留下的回响"),
    SKILL("技巧", "创作、手艺和反复练习留下的回响"),
    RESILIENCE("韧性", "面对困难、调整计划与求助留下的回响"),
    SPIRIT("精神", "主动恢复、调整或求助"),
    CREATION("创造", "做出、改好或练成一样东西");

    val growthTrack: CharacterAttribute?
        get() = when (this) {
            SKILL -> CREATION
            ENDURANCE, RESILIENCE -> null
            else -> this
        }

    companion object {
        val active = listOf(STRENGTH, SPIRIT, KNOWLEDGE, CREATION)
    }
}

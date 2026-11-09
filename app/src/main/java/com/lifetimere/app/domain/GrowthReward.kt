package com.lifetimere.app.domain

enum class GrowthReward(
    val id: String,
    val attribute: CharacterAttribute,
    val threshold: Int,
    val title: String,
) {
    BRACER("bracer", CharacterAttribute.STRENGTH, 1, "远行护腕"),
    BACKPACK("backpack", CharacterAttribute.STRENGTH, 5, "负重背包"),
    LANTERN("lantern", CharacterAttribute.SPIRIT, 1, "守夜灯"),
    BENCH("bench", CharacterAttribute.SPIRIT, 5, "火边小憩"),
    NOTEBOOK("notebook", CharacterAttribute.KNOWLEDGE, 1, "旅人手札"),
    STAR_MAP("star_map", CharacterAttribute.KNOWLEDGE, 5, "星图"),
    SKETCH("sketch", CharacterAttribute.CREATION, 1, "工坊速写"),
    TOOL_BOARD("tool_board", CharacterAttribute.CREATION, 5, "工具挂板"),
}

data class UnlockedGrowthReward(val reward: GrowthReward, val sourceEntryId: Long?)

package com.lifetimere.app.ui.components

import com.lifetimere.app.R
import com.lifetimere.app.domain.GrowthReward

val GrowthReward.art: Int
    get() = when (this) {
        GrowthReward.BRACER -> R.drawable.reward_bracer
        GrowthReward.BACKPACK -> R.drawable.reward_backpack
        GrowthReward.LANTERN -> R.drawable.reward_lantern
        GrowthReward.BENCH -> R.drawable.reward_bench
        GrowthReward.NOTEBOOK -> R.drawable.reward_notebook
        GrowthReward.STAR_MAP -> R.drawable.reward_star_map
        GrowthReward.SKETCH -> R.drawable.reward_sketch
        GrowthReward.TOOL_BOARD -> R.drawable.reward_tool_board
    }

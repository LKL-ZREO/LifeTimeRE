package com.lifetimere.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import com.lifetimere.app.domain.GrowthReward
import com.lifetimere.app.domain.UnlockedGrowthReward
import com.lifetimere.app.ui.components.art
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import com.lifetimere.app.ui.components.ArtTextButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.R
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.model.TravelerProfileStatus
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.components.attributeColor
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelNight

@Composable
fun CharacterScreen(
    attributeTotals: Map<CharacterAttribute, Int>,
    growthSources: List<GrowthSource>,
    travelerProfile: TravelerProfile? = null,
    rewards: List<UnlockedGrowthReward> = emptyList(),
    onViewReward: (GrowthReward) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onReportReview: (Long, ReviewFeedbackReason) -> Unit = { _, _ -> },
) {
    val earnedPoints = CharacterAttribute.active.associateWith { track ->
        attributeTotals.filterKeys { it.growthTrack == track }.values.sum()
    }
    val unlocked = rewards.map { it.reward }.toSet()
    var selectedAttribute by remember { mutableStateOf<CharacterAttribute?>(null) }
    var selectedSource by remember { mutableStateOf<GrowthSource?>(null) }
    Page(
        title = "旅人档案",
        subtitle = "把旅途中获得的成长，留在营地与旅人身边。",
    ) {
        PixelPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.world_traveler_art),
                    contentDescription = "披着青色斗篷的旅人",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 114.dp, height = 152.dp)
                        .background(PixelNight.copy(alpha = .65f))
                        .border(1.dp, PixelGold.copy(alpha = .65f)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    PixelTag("旅人", PixelGold)
                    Text("初行者", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "每一段真实旅程，都会让属性继续成长。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelMuted,
                    )
                }
            }
        }

        Text("成长图鉴", style = MaterialTheme.typography.titleLarge)
        CharacterAttribute.active.forEach { attribute ->
            val trackRewards = GrowthReward.entries.filter { it.attribute == attribute }
            AttributeCard(
                attribute = attribute,
                base = travelerProfile?.tendencies?.get(attribute) ?: 3,
                earned = earnedPoints.getValue(attribute),
                trackRewards = trackRewards,
                unlocked = unlocked,
                onOpenHistory = { selectedAttribute = attribute },
                onViewReward = onViewReward,
            )
        }

        val legacyPoints = listOf(CharacterAttribute.ENDURANCE, CharacterAttribute.RESILIENCE)
            .filter { (attributeTotals[it] ?: 0) > 0 }
        if (legacyPoints.isNotEmpty()) {
            PixelPanel {
                Text("旧旅程", style = MaterialTheme.typography.titleMedium)
                legacyPoints.forEach { attribute ->
                    OutlinedButton(onClick = { selectedAttribute = attribute }) {
                        Text("${attribute.label} +${attributeTotals.getValue(attribute)} · 查看来源")
                    }
                }
            }
        }

        PixelPanel {
            Text(
                when (travelerProfile?.status) {
                    TravelerProfileStatus.COMPLETED -> "旅人登记已完成。重新登记会更新基础值，已获得的成长会保留。"
                    else -> "目前从每项 3 点起步。完成旅人登记，让角色更贴近现在的你。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = PixelMuted,
            )
            OutlinedButton(onClick = onEditProfile, shape = MaterialTheme.shapes.small) {
                Text(if (travelerProfile?.status == TravelerProfileStatus.COMPLETED) "重新登记" else "登记旅人")
            }
        }
    }

    selectedAttribute?.let { attribute ->
        val matchingSources = growthSources.mapNotNull { source ->
            val evidence = source.evidence.filter { it.attribute == attribute || it.attribute.growthTrack == attribute }
            source.copy(evidence = evidence).takeIf { evidence.isNotEmpty() }
        }
        SourceListDialog(
            title = "${attribute.label} · 成长来源",
            summary = if (attribute in CharacterAttribute.active) {
                "基础 ${travelerProfile?.tendencies?.get(attribute) ?: 3} · 旅程成长 +${earnedPoints.getValue(attribute)}"
            } else "旧旅程成长 +${attributeTotals[attribute] ?: 0}",
            sources = matchingSources,
            onDismiss = { selectedAttribute = null },
            onOpen = { selectedSource = it },
        )
    }
    selectedSource?.let { source ->
        SourceDetailDialog(
            source = source,
            onDismiss = { selectedSource = null },
            onReportReview = onReportReview,
        )
    }
}

@Composable
private fun AttributeCard(
    attribute: CharacterAttribute,
    base: Int,
    earned: Int,
    trackRewards: List<GrowthReward>,
    unlocked: Set<GrowthReward>,
    onOpenHistory: () -> Unit,
    onViewReward: (GrowthReward) -> Unit,
) {
    val frame = when (attribute) {
        CharacterAttribute.STRENGTH -> R.drawable.attribute_frame_strength
        CharacterAttribute.SPIRIT -> R.drawable.attribute_frame_spirit
        CharacterAttribute.KNOWLEDGE -> R.drawable.attribute_frame_knowledge
        CharacterAttribute.CREATION -> R.drawable.attribute_frame_creation
        else -> error("Historical attributes have no frame")
    }
    val next = trackRewards.firstOrNull { it !in unlocked }
    val color = attributeColor(attribute)
    Box(Modifier.fillMaxWidth().clickable(onClickLabel = "查看${attribute.label}成长来源", onClick = onOpenHistory)) {
        Image(painterResource(frame), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Column(
            Modifier.fillMaxWidth(.61f).heightIn(min = 145.dp).padding(start = 24.dp, end = 4.dp, top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(attribute.label, style = MaterialTheme.typography.titleLarge, color = color)
            Text(attribute.description, style = MaterialTheme.typography.bodySmall, color = PixelMuted)
            Text(
                next?.let { "${it.title} · ${earned.coerceAtMost(it.threshold)}/${it.threshold}" } ?: "本册奖励已集齐",
                style = MaterialTheme.typography.labelMedium,
                color = PixelGold,
            )
            Text("基础 $base · 成长 +$earned", style = MaterialTheme.typography.labelSmall, color = PixelMuted)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        trackRewards.forEach { reward ->
            val earnedReward = reward in unlocked
            Row(
                modifier = Modifier.weight(1f).then(
                    if (earnedReward) Modifier.clickable(onClickLabel = "在营地查看${reward.title}") { onViewReward(reward) }
                    else Modifier,
                ).padding(horizontal = 4.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(
                    painterResource(reward.art), null,
                    Modifier.size(32.dp).alpha(if (earnedReward) 1f else .65f),
                    colorFilter = if (earnedReward) null else ColorFilter.tint(Color(0xFF71818B)),
                )
                Column {
                    Text(reward.title, style = MaterialTheme.typography.labelMedium, color = PixelGold)
                    Text(if (earnedReward) "已解锁 · 去看看" else "${reward.threshold} 点解锁",
                        style = MaterialTheme.typography.labelSmall, color = PixelMuted)
                }
            }
        }
    }
}

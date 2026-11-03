package com.lifetimere.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.model.TravelerProfileStatus
import com.lifetimere.app.ui.components.AttributeIcon
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelDivider
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.components.PixelTraveler
import com.lifetimere.app.ui.components.attributeColor
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun CharacterScreen(
    attributeTotals: Map<CharacterAttribute, Int>,
    growthSources: List<GrowthSource>,
    travelerProfile: TravelerProfile? = null,
    onEditProfile: () -> Unit = {},
    onReportReview: (Long, ReviewFeedbackReason) -> Unit = { _, _ -> },
) {
    val currentAttributes = CharacterAttribute.entries.associateWith { attribute ->
        (travelerProfile?.tendencies?.get(attribute) ?: 3) + (attributeTotals[attribute] ?: 0)
    }
    val total = currentAttributes.values.sum()
    var selectedAttribute by remember { mutableStateOf<CharacterAttribute?>(null) }
    var selectedSource by remember { mutableStateOf<GrowthSource?>(null) }
    Page(
        title = "旅人档案",
        subtitle = "你做过的每件小事，都会慢慢塑造这名旅人。",
    ) {
        PixelPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PixelTraveler(Modifier.size(width = 114.dp, height = 152.dp))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("角色属性", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text("合计 $total", style = MaterialTheme.typography.labelSmall, color = PixelGold)
        }

        PixelPanel {
            CharacterAttribute.entries.forEachIndexed { index, attribute ->
                AttributeRow(
                    attribute = attribute,
                    points = currentAttributes.getValue(attribute),
                    onClick = { selectedAttribute = attribute },
                )
                if (index != CharacterAttribute.entries.lastIndex) {
                    PixelDivider()
                }
            }
            Text("点击属性，查看成长经历。", style = MaterialTheme.typography.bodySmall, color = PixelMuted)
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
            OutlinedButton(onClick = onEditProfile) {
                Text(if (travelerProfile?.status == TravelerProfileStatus.COMPLETED) "重新登记" else "登记旅人")
            }
        }
    }

    selectedAttribute?.let { attribute ->
        val matchingSources = growthSources.mapNotNull { source ->
            val evidence = source.evidence.filter { it.attribute == attribute }
            source.copy(evidence = evidence).takeIf { evidence.isNotEmpty() }
        }
        SourceListDialog(
            title = "${attribute.label} · ${currentAttributes.getValue(attribute)}",
            summary = "基础 ${travelerProfile?.tendencies?.get(attribute) ?: 3} + 旅程成长 ${attributeTotals[attribute] ?: 0} = 当前 ${currentAttributes.getValue(attribute)}",
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
private fun AttributeRow(attribute: CharacterAttribute, points: Int, onClick: () -> Unit) {
    val color = attributeColor(attribute)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AttributeIcon(attribute)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(attribute.label, style = MaterialTheme.typography.titleMedium, color = PixelParchment)
                Spacer(Modifier.weight(1f))
                Text(points.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge, color = color)
            }
            Text(attribute.description, style = MaterialTheme.typography.bodyMedium, color = PixelMuted)
        }
    }
}

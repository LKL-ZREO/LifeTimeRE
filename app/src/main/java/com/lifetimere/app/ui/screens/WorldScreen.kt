package com.lifetimere.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetimere.app.data.LifeTimeRepository
import com.lifetimere.app.domain.CampMilestones
import com.lifetimere.app.domain.CampState
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelCampScene
import com.lifetimere.app.ui.components.PixelDivider
import com.lifetimere.app.ui.components.PixelMeter
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun WorldScreen(
    journeyDirection: String?,
    campState: CampState,
    onSaveJourneyDirection: (String) -> Unit,
    isVisible: Boolean = true,
    firstEchoSequence: Boolean = false,
    onFirstEchoAnimationComplete: () -> Unit = {},
    onReportReview: (Long, ReviewFeedbackReason) -> Unit = { _, _ -> },
) {
    var showDirectionDialog by remember { mutableStateOf(false) }
    var showCampArchive by remember { mutableStateOf(false) }
    var showWorkshopLocked by remember { mutableStateOf(false) }
    var showFirstEchoMessage by remember { mutableStateOf(false) }
    var sourceTitle by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember { mutableStateOf<GrowthSource?>(null) }

    LaunchedEffect(firstEchoSequence) {
        if (firstEchoSequence) showFirstEchoMessage = true
    }

    Page(
        title = "回响边境",
        subtitle = "旅人和营地，都记得你走过的路。",
    ) {
        PixelPanel(contentPadding = PaddingValues(0.dp)) {
            PixelCampScene(
                stage = campState.stage,
                settledDates = campState.settledDates,
                isVisible = isVisible,
                firstEchoSequence = firstEchoSequence,
                onFirstEchoAnimationComplete = onFirstEchoAnimationComplete,
                onCampfireClick = { showCampArchive = true },
                onSignpostClick = { sourceTitle = "旅程记录" },
                onWorkshopClick = {
                    if (campState.settledDates >= 3) sourceTitle = "工坊的旅志"
                    else showWorkshopLocked = true
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelTag(campState.milestoneName, PixelTeal)
            PixelTag("回顾 ${campState.settledDates} 日", PixelGold)
            Spacer(Modifier.weight(1f))
            Text("区域 01", style = MaterialTheme.typography.labelSmall, color = PixelMuted)
        }

        if (showFirstEchoMessage) {
            PixelPanel {
                Text("第一簇回响", style = MaterialTheme.typography.titleMedium, color = PixelGold)
                Text("你的第一段旅程点亮了篝火。营地记住了这一天。", color = PixelParchment)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showFirstEchoMessage = false
                            showCampArchive = true
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("查看营地档案") }
                    TextButton(onClick = { showFirstEchoMessage = false }) { Text("继续") }
                }
            }
        }

        PixelPanel {
            Text("冒险方向", style = MaterialTheme.typography.titleMedium, color = PixelTeal)
            Text(
                text = journeyDirection ?: "尚未设定。你可以先自由记录，等方向清晰后再填写。",
                style = if (journeyDirection == null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                color = if (journeyDirection == null) PixelMuted else PixelParchment,
            )
            OutlinedButton(
                onClick = { showDirectionDialog = true },
                modifier = Modifier.fillMaxWidth().border(1.dp, PixelTeal, MaterialTheme.shapes.small),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelTeal),
                border = null,
            ) {
                Text(if (journeyDirection == null) "设定方向" else "修改方向")
            }
        }
    }

    if (showCampArchive) {
        CampArchiveDialog(
            campState = campState,
            onDismiss = { showCampArchive = false },
            onViewSources = {
                showCampArchive = false
                sourceTitle = "营地记住的旅志"
            },
        )
    }
    if (showWorkshopLocked) {
        AlertDialog(
            onDismissRequest = { showWorkshopLocked = false },
            title = { Text("工坊尚未苏醒") },
            text = {
                Text(
                    "再完成 ${(3 - campState.settledDates).coerceAtLeast(0)} 个不同日期的回顾，工坊就会亮起。",
                    color = PixelMuted,
                )
            },
            confirmButton = { TextButton(onClick = { showWorkshopLocked = false }) { Text("返回营地") } },
        )
    }

    if (showDirectionDialog) {
        JourneyDirectionDialog(
            currentValue = journeyDirection.orEmpty(),
            onDismiss = { showDirectionDialog = false },
            onConfirm = {
                onSaveJourneyDirection(it)
                showDirectionDialog = false
            },
        )
    }
    sourceTitle?.let { title ->
        SourceListDialog(
            title = title,
            sources = campState.sources,
            onDismiss = { sourceTitle = null },
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
private fun CampArchiveDialog(
    campState: CampState,
    onDismiss: () -> Unit,
    onViewSources: () -> Unit,
) {
    val nextMilestone = campState.nextMilestone
    val remaining = (nextMilestone - campState.settledDates).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("营地档案") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PixelTag(campState.stage.label, PixelGold)
                Text(campState.milestoneName, style = MaterialTheme.typography.headlineSmall, color = PixelParchment)
                Text(
                    when {
                        campState.settledDates == 0 -> "篝火还在等待第一段旅程。完成一次回顾，火光就会在这里留下痕迹。"
                        campState.settledDates == 1 -> "第一簇回响已经点亮篝火和路标。"
                        campState.settledDates == 2 -> "营地正在修整，工坊很快会苏醒。"
                        campState.settledDates == 3 -> "工坊已经苏醒，旅人开始整理一路带回的工具。"
                        campState.settledDates == 5 -> "小径向营地外延伸，新的旅程记忆正在展开。"
                        campState.settledDates >= 7 -> "这一段营地记忆已经成形，下一章正在远处等待。"
                        else -> "每一段完成回顾的旅程，都会留在营地里。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelMuted,
                )
                PixelDivider()
                Text("下一变化：${CampMilestones.nameFor(nextMilestone)}", color = PixelTeal)
                PixelMeter(
                    progress = if (nextMilestone <= 1) 1f else campState.settledDates.toFloat() / nextMilestone,
                    color = PixelGold,
                    segments = 7,
                )
                Text(
                    if (remaining == 0) "营地正在等待下一段旅程。"
                    else "累计 ${campState.settledDates} / $nextMilestone 个有效回顾日，还差 $remaining 天。",
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelMuted,
                )
                TextButton(onClick = onViewSources, modifier = Modifier.fillMaxWidth()) {
                    Text("查看营地记住的旅志")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("返回营地") } },
    )
}

@Composable
private fun JourneyDirectionDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    val presets = listOf("规律运动", "持续学习", "完成作品")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择冒险方向") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("写下你近期想走向的现实目标，也可以暂时留空。")
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(LifeTimeRepository.MAX_DIRECTION_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("${value.length} / ${LifeTimeRepository.MAX_DIRECTION_LENGTH}") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        TextButton(onClick = { value = preset }, modifier = Modifier.weight(1f)) {
                            Text(preset, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("保存") } },
        dismissButton = {
            Row {
                if (currentValue.isNotEmpty()) {
                    TextButton(onClick = { onConfirm("") }) { Text("清除方向") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

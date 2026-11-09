package com.lifetimere.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.lifetimere.app.domain.GrowthReward
import com.lifetimere.app.domain.UnlockedGrowthReward
import com.lifetimere.app.ui.components.art
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.lifetimere.app.ui.components.ArtDialog
import com.lifetimere.app.ui.components.ArtTextButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.lifetimere.app.ui.components.ArtTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetimere.app.data.local.JournalNoteEntity
import com.lifetimere.app.data.local.journalBody
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.data.model.ReviewAttemptStatus
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.ui.LifeTimeViewModel
import com.lifetimere.app.ui.TodayUiState
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.theme.PixelCoral
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit = {},
    rewards: List<UnlockedGrowthReward> = emptyList(),
    onViewReward: (GrowthReward) -> Unit = {},
    onReportReview: (Long, ReviewFeedbackReason) -> Unit = { _, _ -> },
    onSummarize: () -> Unit = {},
    onEditNote: (Long, String) -> Unit = { _, _ -> },
    onDeleteNote: (Long) -> Unit = {},
) {
    var showCorrection by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<JournalNoteEntity?>(null) }
    var deleting by remember { mutableStateOf<JournalNoteEntity?>(null) }
    Page(
        title = "今日旅志",
        subtitle = "完成、尝试、调整或休息，都可以成为今天的一段足迹。",
        action = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "打开设置", tint = PixelGold)
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelTag(uiState.dateLabel)
            PixelTag("已记录 ${uiState.notes.size} 笔", PixelTeal)
        }
        PixelPanel {
            Text("今天想留下什么？", style = MaterialTheme.typography.titleMedium, color = PixelParchment)
            Text("不用整理成完整日记。做成的事、没做完的尝试、临时调整和好好休息，都可以记下来。", color = PixelMuted)
            OutlinedTextField(
                value = uiState.draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("例如：画完一张草图；今天状态不好，决定先休息。") },
                supportingText = { Text("今日已保存 ${uiState.notes.journalBody().length} / 5000 字 · 本次 ${uiState.draft.length} 字") },
            )
            Button(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Text(uiState.buttonLabel)
            }
        }
        uiState.notice?.let { message ->
            PixelPanel { Text(message, color = PixelParchment, style = MaterialTheme.typography.bodyMedium) }
        }
        uiState.savedFootprint?.let { footprint ->
            PixelPanel {
                PixelTag("足迹已进入营地", PixelTeal)
                Text("“${footprint.preview}”", color = PixelParchment)
                Text("今天已留下 ${footprint.noteCount} 笔。夜晚汇总前，它已经安全保存在本机。", color = PixelMuted)
            }
        }
        if (uiState.hasPendingNotes && uiState.review?.attemptStatus == ReviewAttemptStatus.FAILED) {
            PixelPanel {
                PixelTag("上次汇总未完成", PixelCoral)
                Text(uiState.review.failureMessage.ifBlank { "AI 回响暂时没有完成，请稍后重试。" }, color = PixelParchment)
                Text("旅志和上一次有效成长都已保留。", color = PixelMuted)
            }
        }
        PixelPanel {
            PixelTag("营火夜谈 · 每日 ${uiState.settlementTimeLabel}", PixelGold)
            Text(
                when {
                    uiState.isAnalyzing -> "正在汇总全天记录，你仍然可以继续记下一笔。"
                    !uiState.aiConfigured -> "记录已保存在本机，配置 AI 后才会生成全天总结和属性成长。"
                    uiState.notes.isEmpty() -> "今天还没有足迹。第一笔记录会在这里开启今天的旅程。"
                    uiState.hasPendingNotes -> "足迹已保存，等待夜晚汇总。也可以现在提前看一次回响。"
                    else -> "当天回响已完成。继续补记后，会重新计算全天结果，属性不会重复累加。"
                },
                color = PixelParchment,
            )
            Text("后台任务可能受网络和省电影响延后；恢复网络或再次打开应用后会补算。", color = PixelMuted, style = MaterialTheme.typography.bodySmall)
            TextButton(
                onClick = onSummarize,
                enabled = uiState.savedEntry != null && uiState.aiConfigured && !uiState.isAnalyzing && !uiState.isSaving && uiState.hasPendingNotes,
            ) { Text(if (uiState.isAnalyzing) "营火正在倾听……" else "现在汇总已保存的记录", color = PixelGold) }
        }
        val todayRewards = rewards.filter { it.sourceEntryId == uiState.savedEntry?.id && it.sourceEntryId != null }
        AnimatedVisibility(visible = todayRewards.isNotEmpty()) {
            PixelPanel {
                PixelTag("营地藏品已解锁", PixelGold)
                todayRewards.forEach { unlock ->
                    TextButton(onClick = { onViewReward(unlock.reward) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(painterResource(unlock.reward.art), null, Modifier.size(42.dp))
                            Text("${unlock.reward.title} · 去看看", color = PixelGold)
                        }
                    }
                }
            }
        }
        if (uiState.notes.isNotEmpty()) {
            Text("今天的足迹", style = MaterialTheme.typography.titleLarge, color = PixelParchment)
            uiState.notes.asReversed().forEach { note ->
                PixelPanel {
                    val zone = runCatching { ZoneId.of(uiState.savedEntry?.timeZoneId.orEmpty()) }.getOrDefault(ZoneId.systemDefault())
                    PixelTag(Instant.ofEpochMilli(note.createdAt).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")), PixelTeal)
                    Text(note.body, style = MaterialTheme.typography.bodyLarge, color = PixelParchment)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { editing = note }) { Text("编辑", color = PixelGold) }
                        TextButton(onClick = { deleting = note }) { Text("删除", color = PixelCoral) }
                    }
                }
            }
        }
        uiState.review?.takeIf { it.status == ReviewStatus.COMPLETE }?.let { review ->
            PixelPanel {
                PixelTag(if (uiState.hasPendingNotes) "上次回响 · 有新记录待汇总" else "今日回响", PixelTeal)
                Text(review.summary, color = PixelParchment)
                if (review.evidence.isEmpty()) Text("今天没有足够明确的行动依据，属性未变化。记录本身仍然有效。", color = PixelMuted)
                review.evidence.forEach { evidence ->
                    Text("${evidence.attribute.label} +${evidence.points} · ${evidence.explanation}", color = PixelGold)
                    Text("原文：${evidence.quote}", color = PixelMuted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { showCorrection = true }, enabled = !uiState.isAnalyzing) { Text("判断不准确", color = PixelGold) }
            }
        }
    }
    editing?.let { note ->
        var body by remember(note.id) { mutableStateOf(note.body) }
        ArtDialog(
        art = com.lifetimere.app.R.drawable.world_tent_panel,
            onDismissRequest = { editing = null },
            title = { Text("修改这笔足迹") },
            text = { OutlinedTextField(value = body, onValueChange = { body = it.take(LifeTimeViewModel.MAX_DRAFT_LENGTH) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { onEditNote(note.id, body); editing = null }, enabled = body.isNotBlank() && body.trim() != note.body) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }
    deleting?.let { note ->
        ArtDialog(
        art = com.lifetimere.app.R.drawable.world_tent_panel,
            onDismissRequest = { deleting = null },
            title = { Text("删除这笔足迹？") },
            text = { Text("删除后会按剩余记录重新汇总；删除当天最后一笔时，当天回响和成长也会一起清除。") },
            confirmButton = { TextButton(onClick = { onDeleteNote(note.id); deleting = null }) { Text("删除", color = PixelCoral) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("保留") } },
        )
    }
    if (showCorrection) ReviewCorrectionDialog(
        onDismiss = { showCorrection = false },
        onEdit = { showCorrection = false },
        onSubmit = { reason -> uiState.savedEntry?.let { onReportReview(it.id, reason) }; showCorrection = false },
    )
}

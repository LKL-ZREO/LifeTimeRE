package com.lifetimere.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        subtitle = "做完一件，记下一笔。夜晚的营火会把足迹编成回响。",
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
            Text("刚刚完成了什么？", style = MaterialTheme.typography.titleMedium, color = PixelParchment)
            Text("不用等到睡前，也不用整理成日记。每次只写一件事就好。", color = PixelMuted)
            OutlinedTextField(
                value = uiState.draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("例如：刚跑完三公里，最后一公里有点吃力。") },
                supportingText = { Text("今日已保存 ${uiState.notes.journalBody().length} / 5000 字 · 本次 ${uiState.draft.length} 字") },
            )
            Button(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(uiState.buttonLabel)
            }
        }
        uiState.notice?.let { message ->
            PixelPanel { Text(message, color = PixelParchment, style = MaterialTheme.typography.bodyMedium) }
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
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("修改这笔足迹") },
            text = { OutlinedTextField(value = body, onValueChange = { body = it.take(LifeTimeViewModel.MAX_DRAFT_LENGTH) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { onEditNote(note.id, body); editing = null }, enabled = body.isNotBlank() && body.trim() != note.body) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }
    deleting?.let { note ->
        AlertDialog(
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

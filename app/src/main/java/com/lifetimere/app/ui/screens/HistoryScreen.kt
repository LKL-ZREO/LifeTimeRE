package com.lifetimere.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.ui.LifeTimeViewModel
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelDivider
import com.lifetimere.app.ui.components.PixelJournalIcon
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.theme.PixelCoral
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelOutline
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun HistoryScreen(
    entries: List<DailyEntry>,
    reviewStatuses: Map<Long, ReviewStatus>,
    onWriteClick: () -> Unit,
    onEditEntry: (Long, String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    sources: Map<Long, GrowthSource> = emptyMap(),
    onSummarize: (Long) -> Unit = {},
) {
    var editingEntry by remember { mutableStateOf<DailyEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<DailyEntry?>(null) }

    Page(
        title = "旅程回顾",
        subtitle = "每一篇旅志，都是你走过这片世界的真实证据。",
    ) {
        if (entries.isEmpty()) {
            EmptyHistory(onWriteClick)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("旅志时间线", style = MaterialTheme.typography.titleLarge)
                PixelTag("${entries.size} 篇旅志", PixelTeal)
            }
            entries.forEach { entry ->
                EntryPanel(
                    entry = entry,
                    reviewStatus = reviewStatuses[entry.id],
                    onEdit = { editingEntry = entry },
                    onDelete = { deletingEntry = entry },
                    source = sources[entry.id],
                    onSummarize = { onSummarize(entry.id) },
                )
            }
        }
    }

    editingEntry?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { body ->
                onEditEntry(entry.id, body)
                editingEntry = null
            },
        )
    }

    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("删除这篇旅志？") },
            text = { Text("${formatDate(entry)}的记录将从本机删除。后续产生的回响也会随记录一起清理。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(entry.id)
                        deletingEntry = null
                    },
                ) { Text("删除", color = PixelCoral) }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) { Text("保留") }
            },
        )
    }
}

@Composable
private fun EmptyHistory(onWriteClick: () -> Unit) {
    PixelPanel {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PixelJournalIcon()
            PixelTag("0 篇旅志")
            Text("日志还是空白的", style = MaterialTheme.typography.headlineSmall, color = PixelParchment)
            Text(
                text = "写下今天的旅程，第一段冒险记录就会出现在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelMuted,
            )
            OutlinedButton(
                onClick = onWriteClick,
                modifier = Modifier.fillMaxWidth().border(1.dp, PixelGold, MaterialTheme.shapes.small),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelGold),
                border = null,
            ) {
                Text("去写今天", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EntryPanel(entry: DailyEntry, reviewStatus: ReviewStatus?, onEdit: () -> Unit, onDelete: () -> Unit, source: GrowthSource?, onSummarize: () -> Unit) {
    PixelPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDate(entry), style = MaterialTheme.typography.titleMedium, color = PixelParchment)
            PixelTag("第 ${entry.revision} 版", PixelGold)
        }
        PixelTag(
            when (reviewStatus) {
                ReviewStatus.COMPLETE -> "AI 回响已完成"
                ReviewStatus.RUNNING -> "AI 回响生成中"
                ReviewStatus.FAILED -> "AI 回响失败"
                else -> "已保存 · 待回响"
            },
            if (reviewStatus == ReviewStatus.COMPLETE) PixelTeal else PixelMuted,
        )
        PixelDivider()
        Text(entry.body, style = MaterialTheme.typography.bodyLarge, color = PixelParchment)
        source?.let {
            PixelDivider()
            PixelTag(if (reviewStatus == ReviewStatus.COMPLETE) "全天回响" else "上次回响 · 有修改待汇总", PixelTeal)
            Text(it.summary, color = PixelParchment)
            it.evidence.groupBy { evidence -> evidence.attribute }.forEach { (attribute, evidence) ->
                Text("${attribute.label} +${evidence.sumOf { row -> row.points }}", color = PixelGold)
            }
        }
        if (reviewStatus != ReviewStatus.COMPLETE) TextButton(onClick = onSummarize) { Text("汇总这一天", color = PixelGold) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, PixelOutline),
            ) {
                Text("编辑")
            }
            TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text("删除", color = PixelCoral)
            }
        }
    }
}

@Composable
private fun EditEntryDialog(
    entry: DailyEntry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var body by remember(entry.id, entry.revision) { mutableStateOf(entry.body) }
    val normalizedBody = body.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 ${formatDate(entry)}") },
        text = {
            Column {
            Text("保存后会把当天内容合并为一笔记录，重新汇总成功后更新当天属性。", color = PixelMuted)
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(LifeTimeViewModel.MAX_DRAFT_LENGTH) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                supportingText = { Text("${body.length} / ${LifeTimeViewModel.MAX_DRAFT_LENGTH}") },
            )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(normalizedBody) },
                enabled = normalizedBody.isNotEmpty() && normalizedBody != entry.body,
            ) { Text("保存修改") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatDate(entry: DailyEntry): String =
    "${entry.localDate.year}年${entry.localDate.monthValue}月${entry.localDate.dayOfMonth}日"

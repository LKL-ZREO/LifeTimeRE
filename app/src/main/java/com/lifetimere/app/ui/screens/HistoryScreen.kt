package com.lifetimere.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lifetimere.app.ui.components.ArtDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.lifetimere.app.ui.components.ArtTextButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.lifetimere.app.ui.components.ArtTextButton as TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.R
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.WeeklyReview
import com.lifetimere.app.domain.WeeklyReviewItem
import com.lifetimere.app.ui.LifeTimeViewModel
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelDivider
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
    reviewFailures: Map<Long, String> = emptyMap(),
    onWriteClick: () -> Unit,
    onEditEntry: (Long, String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    sources: Map<Long, GrowthSource> = emptyMap(),
    weeklyReview: WeeklyReview,
    onSummarize: (Long) -> Unit = {},
) {
    var editingEntry by remember { mutableStateOf<DailyEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<DailyEntry?>(null) }
    var weeklySource by remember { mutableStateOf<WeeklyReviewItem?>(null) }

    Page(
        title = "旅程回顾",
        subtitle = "每一篇旅志，都是你走过这片世界的真实证据。",
    ) {
        if (weeklyReview.recordedDays > 0) {
            WeeklyReviewPanel(weeklyReview, onOpenSource = { weeklySource = it })
        }
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
                    failureMessage = reviewFailures[entry.id],
                    onEdit = { editingEntry = entry },
                    onDelete = { deletingEntry = entry },
                    source = sources[entry.id],
                    onSummarize = { onSummarize(entry.id) },
                )
            }
        }
    }

    weeklySource?.let { item ->
        WeeklySourceDialog(item = item, onDismiss = { weeklySource = null })
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
        ArtDialog(
        art = com.lifetimere.app.R.drawable.world_atlas_panel,
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
            Image(
                painter = painterResource(R.drawable.page_journal_icon),
                contentDescription = "一本等待书写的旅行日志",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(112.dp),
            )
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
private fun EntryPanel(entry: DailyEntry, reviewStatus: ReviewStatus?, failureMessage: String?, onEdit: () -> Unit, onDelete: () -> Unit, source: GrowthSource?, onSummarize: () -> Unit) {
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
        if (reviewStatus == ReviewStatus.FAILED) {
            Text(failureMessage ?: "上次汇总没有完成，旅志仍保存在本机。", color = PixelCoral)
        }
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
private fun WeeklyReviewPanel(review: WeeklyReview, onOpenSource: (WeeklyReviewItem) -> Unit) {
    PixelPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("本周旅程", style = MaterialTheme.typography.titleLarge, color = PixelParchment)
            PixelTag("${review.recordedDays} 个记录日", PixelTeal)
        }
        Text(
            "${review.startDate.monthValue}月${review.startDate.dayOfMonth}日—${review.endDate.monthValue}月${review.endDate.dayOfMonth}日 · " +
                "已完成 ${review.reviewedDays} 日回响 · 成长 +${review.totalGrowth}",
            color = PixelMuted,
        )
        if (review.pendingDays > 0) {
            Text("还有 ${review.pendingDays} 日等待重新汇总，完成后会自动进入本周回顾。", color = PixelGold)
        }
        if (review.items.isEmpty()) {
            Text("本周的足迹已经留下，完成每日回响后会在这里形成有原文出处的回顾。", color = PixelMuted)
        } else {
            val (growthDays, rememberedDays) = review.items.partition { it.growthPoints > 0 }
            WeeklyItemGroup("本周有成长依据的经历", growthDays, onOpenSource)
            WeeklyItemGroup("营地也记住了这些普通日子", rememberedDays, onOpenSource)
        }
    }
}

@Composable
private fun WeeklyItemGroup(
    title: String,
    items: List<WeeklyReviewItem>,
    onOpenSource: (WeeklyReviewItem) -> Unit,
) {
    if (items.isEmpty()) return
    PixelDivider()
    Text(title, style = MaterialTheme.typography.titleMedium, color = PixelTeal)
    items.forEach { item ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PixelTag("${item.date.monthValue}月${item.date.dayOfMonth}日", PixelGold)
            if (item.growthPoints > 0) PixelTag("成长 +${item.growthPoints}", PixelTeal)
            else PixelTag("完成回顾", PixelMuted)
        }
        Text(item.summary, color = PixelParchment)
        item.quotes.forEach { quote ->
            Text("原文：“$quote”", color = PixelMuted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { onOpenSource(item) }) { Text("查看当天原文", color = PixelGold) }
    }
}

@Composable
private fun WeeklySourceDialog(item: WeeklyReviewItem, onDismiss: () -> Unit) {
    ArtDialog(
        art = com.lifetimere.app.R.drawable.world_atlas_panel,
        onDismissRequest = onDismiss,
        title = { Text("${item.date.monthValue}月${item.date.dayOfMonth}日 · 原文") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("周回顾引用的部分已高亮。", color = PixelMuted)
                Text(highlightQuotes(item.fullBody, item.quotes), color = PixelParchment)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("返回周回顾") } },
    )
}

private fun highlightQuotes(body: String, quotes: List<String>): AnnotatedString {
    val ranges = quotes.mapNotNull { quote ->
        body.indexOf(quote).takeIf { it >= 0 }?.let { it until (it + quote.length) }
    }.sortedBy { it.first }
    if (ranges.isEmpty()) return AnnotatedString(body)
    return buildAnnotatedString {
        var cursor = 0
        ranges.forEach { range ->
            if (range.first < cursor) return@forEach
            append(body.substring(cursor, range.first))
            withStyle(SpanStyle(color = PixelGold, fontWeight = FontWeight.Bold)) {
                append(body.substring(range.first, range.last + 1))
            }
            cursor = range.last + 1
        }
        append(body.substring(cursor))
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
    ArtDialog(
        art = com.lifetimere.app.R.drawable.world_atlas_panel,
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

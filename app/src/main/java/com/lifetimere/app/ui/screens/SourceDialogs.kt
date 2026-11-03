package com.lifetimere.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetimere.app.domain.GrowthSource
import com.lifetimere.app.domain.ReviewFeedbackReason
import com.lifetimere.app.ui.components.PixelDivider
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun SourceListDialog(
    title: String,
    sources: List<GrowthSource>,
    onDismiss: () -> Unit,
    onOpen: (GrowthSource) -> Unit,
    summary: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                summary?.let {
                    Text(it, color = PixelTeal)
                    PixelDivider()
                }
                if (sources.isEmpty()) {
                    Text("目前还没有可追溯的旅志。", color = PixelMuted)
                }
                sources.forEach { source ->
                    TextButton(onClick = { onOpen(source) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatSourceDate(source), style = MaterialTheme.typography.titleMedium, color = PixelGold)
                            Text(
                                source.entry.body.replace('\n', ' ').take(70),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PixelParchment,
                            )
                            val points = source.evidence.sumOf { it.points }
                            PixelTag(if (points == 0) "完成回顾 · 零成长" else "成长 +$points", PixelTeal)
                        }
                    }
                    PixelDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
fun SourceDetailDialog(
    source: GrowthSource,
    onDismiss: () -> Unit,
    onReportReview: ((Long, ReviewFeedbackReason) -> Unit)? = null,
) {
    var showCorrectionDialog by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatSourceDate(source)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PixelTag("第 ${source.entry.revision} 版旅志")
                Text(source.entry.body, style = MaterialTheme.typography.bodyLarge, color = PixelParchment)
                if (source.summary.isNotBlank()) {
                    PixelDivider()
                    Text(source.summary, style = MaterialTheme.typography.bodyMedium, color = PixelTeal)
                }
                if (source.evidence.isEmpty()) {
                    Text("这一天完成了回顾，但没有足够明确的行动依据。", color = PixelMuted)
                } else {
                    PixelDivider()
                    source.evidence.forEach { evidence ->
                        Text("${evidence.attribute.label} +${evidence.points}", style = MaterialTheme.typography.titleMedium, color = PixelGold)
                        Text("原文：${evidence.quote}", color = PixelParchment)
                        Text(evidence.explanation, style = MaterialTheme.typography.bodyMedium, color = PixelMuted)
                    }
                }
                PixelDivider()
                Text("规则 v${source.ruleVersion}", style = MaterialTheme.typography.labelSmall, color = PixelTeal)
                Text(
                    "模型 ${source.metadata.model.ifBlank { "历史版本未记录" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelMuted,
                )
                Text(
                    "Prompt ${source.metadata.promptVersion.ifBlank { "未记录" }} · Schema ${source.metadata.schemaVersion.ifBlank { "未记录" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelMuted,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("返回") } },
        dismissButton = onReportReview?.let {
            { TextButton(onClick = { showCorrectionDialog = true }) { Text("判断不准确", color = PixelGold) } }
        },
    )

    if (showCorrectionDialog) {
        ReviewCorrectionDialog(
            onDismiss = { showCorrectionDialog = false },
            onEdit = {
                showCorrectionDialog = false
                onDismiss()
            },
            onSubmit = { reason ->
                onReportReview?.invoke(source.entry.id, reason)
                showCorrectionDialog = false
                onDismiss()
            },
        )
    }
}

@Composable
fun ReviewCorrectionDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSubmit: (ReviewFeedbackReason) -> Unit,
) {
    var selected by remember { mutableStateOf(ReviewFeedbackReason.EVIDENCE_MISMATCH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("这次判断哪里不准确？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("你可以修改旅志后重新生成，或只标记问题并让 AI 重新分析。属性和点数仍由系统规则决定。", color = PixelMuted)
                ReviewFeedbackReason.entries.forEach { reason ->
                    OutlinedButton(
                        onClick = { selected = reason },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selected == reason) "[已选择] ${reason.label}" else reason.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) { Text("记录并重新分析") }
        },
        dismissButton = {
            TextButton(onClick = onEdit) { Text("修改旅志") }
        },
    )
}

private fun formatSourceDate(source: GrowthSource): String = source.entry.localDate.let {
    "${it.year}年${it.monthValue}月${it.dayOfMonth}日"
}

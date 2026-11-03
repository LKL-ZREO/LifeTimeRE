package com.lifetimere.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifetimere.app.data.model.TravelerProfile
import com.lifetimere.app.data.model.TravelerProfileStatus
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.domain.TravelerProfileScorer
import com.lifetimere.app.domain.TravelerQuestionnaire
import com.lifetimere.app.ui.components.AttributeIcon
import com.lifetimere.app.ui.components.EchoRune
import com.lifetimere.app.ui.components.PixelDivider
import com.lifetimere.app.ui.components.PixelMeter
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.components.attributeColor
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelInk
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun TravelerOnboardingScreen(
    profile: TravelerProfile?,
    onBegin: () -> Unit,
    onAnswer: (String, String) -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onRetake: () -> Unit,
    attributeGrowth: Map<CharacterAttribute, Int> = emptyMap(),
) {
    Box(Modifier.fillMaxSize().background(PixelNight)) {
        when {
            profile == null -> RegistrationIntro(onBegin = onBegin, onSkip = onSkip)
            profile.status == TravelerProfileStatus.IN_PROGRESS || profile.status == TravelerProfileStatus.RETAKING -> {
                if (profile.questionIndex >= TravelerQuestionnaire.QUESTION_COUNT) {
                    ProfileReveal(
                        tendencies = TravelerProfileScorer.score(profile.answers),
                        onComplete = onComplete,
                        onRetake = onRetake,
                        onSkip = onSkip,
                        attributeGrowth = attributeGrowth,
                    )
                } else {
                    val question = TravelerQuestionnaire.question(profile.questionIndex)
                    QuestionStep(
                        index = profile.questionIndex,
                        question = question.prompt,
                        options = question.options,
                        selectedOptionId = profile.answers[question.id],
                        onAnswer = { onAnswer(question.id, it) },
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                    )
                }
            }
            else -> RegistrationIntro(onBegin = onBegin, onSkip = onSkip)
        }
    }
}

@Composable
private fun RegistrationIntro(onBegin: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EchoRune(24.dp)
            Text("边境旅人档案", style = MaterialTheme.typography.titleMedium, color = PixelGold)
        }
        Spacer(Modifier.height(8.dp))
        Text("篝火还不认识你。", style = MaterialTheme.typography.headlineLarge, color = PixelParchment)
        Text(
            "先不问你想成为谁，只记下你最近是怎样生活的。",
            style = MaterialTheme.typography.bodyLarge,
            color = PixelMuted,
        )
        PixelPanel {
            Text("六个小选择，约一分钟", style = MaterialTheme.typography.titleMedium, color = PixelTeal)
            Text("没有标准答案，也不需要输入文字。你的选择会成为角色的起始属性，之后会随旅程继续成长。", color = PixelMuted)
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBegin, modifier = Modifier.fillMaxWidth()) {
            Text("开始登记")
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("稍后再说", color = PixelMuted)
        }
    }
}

@Composable
private fun QuestionStep(
    index: Int,
    question: String,
    options: List<com.lifetimere.app.domain.TravelerOption>,
    selectedOptionId: String?,
    onAnswer: (String) -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("旅人登记", style = MaterialTheme.typography.titleMedium, color = PixelGold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) { Text("跳过", color = PixelMuted) }
        }
        Text("${index + 1} / ${TravelerQuestionnaire.QUESTION_COUNT}", style = MaterialTheme.typography.labelLarge, color = PixelTeal)
        PixelMeter(
            progress = (index + 1).toFloat() / TravelerQuestionnaire.QUESTION_COUNT,
            color = PixelGold,
            segments = TravelerQuestionnaire.QUESTION_COUNT,
        )
        Text(question, style = MaterialTheme.typography.headlineSmall, color = PixelParchment)
        Text("选择最接近你最近情况的一项", style = MaterialTheme.typography.bodyMedium, color = PixelMuted)
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            options.forEach { option ->
                val selected = selectedOptionId == option.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) PixelGold.copy(alpha = 0.14f) else PixelInk)
                        .semantics {
                            contentDescription = option.label
                        }
                        .selectable(selected = selected, role = Role.RadioButton) { onAnswer(option.id) }
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (selected) "●" else "○", color = if (selected) PixelGold else PixelMuted)
                    Spacer(Modifier.width(12.dp))
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, color = PixelParchment)
                }
            }
        }
        TextButton(onClick = onPrevious, enabled = index > 0, modifier = Modifier.align(Alignment.Start)) {
            Text("上一题", color = if (index > 0) PixelTeal else PixelMuted)
        }
    }
}

@Composable
private fun ProfileReveal(
    tendencies: Map<CharacterAttribute, Int>,
    onComplete: () -> Unit,
    onRetake: () -> Unit,
    onSkip: () -> Unit,
    attributeGrowth: Map<CharacterAttribute, Int>,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PixelTag("旅人登记", PixelGold)
        Text("你的角色属性", style = MaterialTheme.typography.headlineLarge, color = PixelParchment)
        Text(
            "这些属性由刚才的选择形成，不是对现实能力的定论。今后的真实行动会继续让角色成长。",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelMuted,
        )
        PixelPanel {
            CharacterAttribute.entries.forEachIndexed { index, attribute ->
                Row(
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "${attribute.label} ${(tendencies[attribute] ?: 3) + (attributeGrowth[attribute] ?: 0)}"
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AttributeIcon(attribute)
                    Text(attribute.label, modifier = Modifier.weight(1f), color = PixelParchment)
                    Text("${(tendencies[attribute] ?: 3) + (attributeGrowth[attribute] ?: 0)}", color = attributeColor(attribute))
                }
                if (index != CharacterAttribute.entries.lastIndex) PixelDivider()
            }
        }
        Text("确认后使用这组属性。已有的旅程成长已计入；重新登记不会清除成长。", style = MaterialTheme.typography.labelMedium, color = PixelTeal)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("进入营地") }
        TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("重新回答", color = PixelTeal) }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("稍后再说", color = PixelMuted) }
    }
}

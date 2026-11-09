package com.lifetimere.app.ui.screens

import android.database.sqlite.SQLiteException
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import com.lifetimere.app.ui.components.ArtDialog
import com.lifetimere.app.ui.components.ArtPanel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.lifetimere.app.ui.components.ArtTextButton as TextButton
import androidx.compose.runtime.rememberCoroutineScope
import com.lifetimere.app.data.local.ActionFollowupEntity
import com.lifetimere.app.data.local.KeepsakeEntity
import com.lifetimere.app.data.local.ProjectEntity
import com.lifetimere.app.data.local.ProjectEntryEntity
import com.lifetimere.app.data.local.ProjectMilestoneEntity
import com.lifetimere.app.data.model.DailyEntry
import com.lifetimere.app.data.model.ReviewStatus
import com.lifetimere.app.domain.GrowthSource
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.lifetimere.app.domain.GrowthReward
import com.lifetimere.app.domain.UnlockedGrowthReward
import com.lifetimere.app.ui.components.art
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifetimere.app.R
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.ui.theme.PixelParchment

private enum class WorldPlace(val title: String, val hint: String, val art: Int) {
    CAMP("回响营地", "轻触旅人、工坊、篝火或远方的小径", R.drawable.world_camp_unlit_art),
    WORKSHOP("微光工坊", "轻触木牌、纸页或布片，管理你的项目", R.drawable.world_workshop_art),
    PATH("远望小径", "轻触石刻或打开旅程地图", R.drawable.world_path_art),
}

private enum class CampPanel { FIRE, WORKBENCH, FOLLOWUPS, PATH, CHAPTER }

private data class RewardPlacement(val place: WorldPlace, val x: Float, val y: Float, val size: Int)

private val GrowthReward.placement: RewardPlacement
    get() = when (this) {
        GrowthReward.BRACER -> RewardPlacement(WorldPlace.CAMP, .72f, .78f, 40)
        GrowthReward.BACKPACK -> RewardPlacement(WorldPlace.CAMP, .86f, .78f, 48)
        GrowthReward.LANTERN -> RewardPlacement(WorldPlace.CAMP, .11f, .55f, 44)
        GrowthReward.BENCH -> RewardPlacement(WorldPlace.CAMP, .17f, .65f, 66)
        GrowthReward.NOTEBOOK -> RewardPlacement(WorldPlace.WORKSHOP, .17f, .71f, 46)
        GrowthReward.STAR_MAP -> RewardPlacement(WorldPlace.WORKSHOP, .29f, .38f, 64)
        GrowthReward.SKETCH -> RewardPlacement(WorldPlace.WORKSHOP, .69f, .32f, 54)
        GrowthReward.TOOL_BOARD -> RewardPlacement(WorldPlace.WORKSHOP, .52f, .38f, 62)
    }

@Composable
fun WorldCampScreen(
    date: LocalDate,
    entries: List<DailyEntry>,
    sources: List<GrowthSource>,
    reviewStatuses: Map<Long, ReviewStatus>,
    reviewFailures: Map<Long, String>,
    keepsakes: List<KeepsakeEntity>,
    projects: List<ProjectEntity>,
    actionFollowups: List<ActionFollowupEntity>,
    onCreateAction: suspend (Long, String, LocalDate) -> Boolean,
    onRescheduleAction: suspend (Long, LocalDate) -> Boolean,
    onCompleteAction: suspend (Long, String, String) -> Boolean,
    projectMilestones: List<ProjectMilestoneEntity>,
    projectEntries: List<ProjectEntryEntity>,
    onCreateProject: suspend (String) -> Long?,
    onAddMilestone: suspend (Long, String) -> Boolean,
    onCompleteMilestone: suspend (Long, Boolean) -> Boolean,
    onSetNextStep: suspend (Long, String) -> Boolean,
    onSetProjectEntry: suspend (Long, Long, Boolean) -> Boolean,
    aiConfigured: Boolean,
    onRetry: (Long) -> Unit,
    onDisplay: suspend (Long?, Boolean) -> Boolean,
    onOpenSettings: () -> Unit,
    rewards: List<UnlockedGrowthReward> = emptyList(),
    requestedRewardId: String? = null,
    onRewardRequestHandled: () -> Unit = {},
) {
    var place by rememberSaveable { mutableStateOf(WorldPlace.CAMP) }
    var message by remember { mutableStateOf("旅人已经回到营地。点亮一处物件，看看它怎样回应。") }
    var selectionTick by remember { mutableStateOf(0) }
    var panel by remember { mutableStateOf<CampPanel?>(null) }
    var workshopTab by remember { mutableStateOf(0) }
    var rewardDetailId by remember { mutableStateOf<String?>(null) }
    var wearingPack by rememberSaveable { mutableStateOf(false) }
    var resting by rememberSaveable { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var detailKeepsake by remember { mutableStateOf<Long?>(null) }
    var writing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val journal = remember(entries) { entries.associateBy { it.id } }
    val echoes = remember(journal, sources) {
        sources.filter { source ->
            val entry = journal[source.entry.id]
            entry != null && entry.body.isNotBlank() && entry.body == source.entry.body &&
                entry.revision == source.entry.revision
        }.distinctBy { it.entry.localDate }.sortedByDescending { it.entry.localDate }
    }
    val firstChapter = echoes.asReversed().take(7)
    val today = entries.firstOrNull { it.localDate == date }
    val todayEcho = echoes.firstOrNull { it.entry.localDate == date }
    val status = today?.let { reviewStatuses[it.id] }
    val validKeepsakes = keepsakes.filter { item -> journal[item.entryId]?.body?.contains(item.quote) == true }
    val fireLit = echoes.isNotEmpty()

    LaunchedEffect(requestedRewardId, rewards) {
        val requested = rewards.firstOrNull { it.reward.id == requestedRewardId } ?: return@LaunchedEffect
        place = requested.reward.placement.place
        rewardDetailId = requested.reward.id
        onRewardRequestHandled()
    }

    fun openPanel(next: CampPanel) {
        error = null
        panel = next
    }

    fun saveSelection(action: suspend () -> Boolean, onSaved: () -> Unit) {
        if (writing) return
        writing = true
        error = null
        scope.launch {
            try {
                if (action()) onSaved() else error = "原文已变更，请重新选择。"
            } catch (_: SQLiteException) {
                error = "保存失败，请重试。"
            } finally {
                writing = false
            }
        }
    }

    BackHandler(enabled = place != WorldPlace.CAMP && panel == null && detailId == null && detailKeepsake == null) {
        place = WorldPlace.CAMP
        message = "回到营地。"
    }
    BackHandler(enabled = rewardDetailId != null) { rewardDetailId = null }
    val transition = rememberInfiniteTransition(label = "世界呼吸")
    val pulse by transition.animateFloat(
        initialValue = .72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1250), RepeatMode.Reverse), label = "光点",
    )
    val travelerBob by transition.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "旅人呼吸",
    )
    Box(Modifier.fillMaxSize().background(PixelNight)) {
        Crossfade(targetState = place, animationSpec = tween(450), label = "切换场景") { scene ->
            when (scene) {
                WorldPlace.CAMP -> CampfireScene(fireLit)
                WorldPlace.WORKSHOP -> WorkshopScene(listOf("木牌", "纸页", "布片")[workshopTab.coerceIn(0, 2)], selectionTick, 0, projects.isNotEmpty())
                else -> Image(
                    painter = painterResource(scene.art), contentDescription = "${scene.title}的日式像素场景",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(154.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color(0xEA101B24), Color(0x99101B24), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(210.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB5101B24), Color(0xF5101B24)))))

        BoxWithConstraints(Modifier.fillMaxSize()) {
            when (place) {
                WorldPlace.CAMP -> {
                    SceneHotspot("工坊", .62f, .30f, .37f, .32f, maxWidth, maxHeight, pulse) {
                        place = WorldPlace.WORKSHOP
                        message = "工坊里可以管理项目、里程碑和相关旅志。"
                    }
                    SceneHotspotFromArt(
                        label = "查看篝火回响",
                        sourceBounds = Rect(380f, 985f, 680f, 1200f),
                        artWidth = 1024f,
                        artHeight = 1536f,
                        screenWidth = maxWidth,
                        screenHeight = maxHeight,
                        pulse = pulse,
                    ) {
                        openPanel(CampPanel.FIRE)
                    }
                    SceneHotspot("小径", .35f, .25f, .30f, .22f, maxWidth, maxHeight, pulse) {
                        if (echoes.size >= 5) {
                            place = WorldPlace.PATH
                            message = "小径上的石刻对应走过的日子。"
                        } else {
                            message = "累计完成 5 天回响后，小径会开放。当前 ${echoes.size}/5 天。"
                        }
                    }
                    val travelerArt = when {
                        resting -> R.drawable.world_traveler_seated
                        wearingPack -> R.drawable.world_traveler_equipped
                        else -> R.drawable.world_traveler_art
                    }
                    Image(
                        painter = painterResource(travelerArt),
                        contentDescription = if (resting) "旅人正在小憩，点击起身" else "旅人，点击打招呼",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(
                                maxWidth * (if (resting) .18f else .72f),
                                maxHeight * (if (resting) .63f else .70f) + travelerBob.dp,
                            )
                            .size(maxWidth * (if (resting) .25f else .20f), maxHeight * .19f)
                            .clickable {
                                if (resting) {
                                    resting = false
                                    message = "旅人站起来，拍掉斗篷上的松针。"
                                } else {
                                    message = "旅人回头向你挥手：这里还会继续记住你的旅程。"
                                }
                            },
                    )
                }
                WorldPlace.WORKSHOP -> {
                    SceneHotspot("木牌 · 项目", .13f, .54f, .23f, .15f, maxWidth, maxHeight, pulse, workshopTab == 0, showMarker = true) {
                        workshopTab = 0; selectionTick++; openPanel(CampPanel.WORKBENCH)
                    }
                    SceneHotspot("纸页 · 旅志", .37f, .54f, .23f, .15f, maxWidth, maxHeight, pulse, workshopTab == 1, showMarker = true) {
                        workshopTab = 1; selectionTick++; openPanel(CampPanel.WORKBENCH)
                    }
                    SceneHotspot("布片 · 里程碑", .60f, .54f, .24f, .15f, maxWidth, maxHeight, pulse, workshopTab == 2, showMarker = true) {
                        workshopTab = 2; selectionTick++; openPanel(CampPanel.WORKBENCH)
                    }
                }
                WorldPlace.PATH -> {
                    listOf(
                        Triple(.61f, .31f, 1), Triple(.38f, .56f, 2), Triple(.32f, .75f, 3),
                    ).forEach { (x, y, day) ->
                        val echo = echoes.getOrNull(day - 1)
                        if (echo != null) {
                            SceneHotspot(echo.entry.localDate.toString(), x, y, .27f, .12f, maxWidth, maxHeight, pulse) {
                                detailId = echo.entry.id
                            }
                        }
                    }
                }
            }
            rewards.forEach { unlocked ->
                val reward = unlocked.reward
                val placement = reward.placement
                if (placement.place == place && !(resting && reward == GrowthReward.BENCH)) {
                    Image(
                        painter = painterResource(reward.art),
                        contentDescription = reward.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.offset(maxWidth * placement.x, maxHeight * placement.y)
                            .size(placement.size.dp)
                            .clickable(onClickLabel = "查看${reward.title}") { rewardDetailId = reward.id },
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 14.dp, end = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (place != WorldPlace.CAMP) {
                    Text("‹", Modifier.clip(CircleShape).background(Color(0xCC172831))
                        .clickable { place = WorldPlace.CAMP; message = "回到营地。" }
                        .padding(horizontal = 12.dp, vertical = 1.dp),
                        color = PixelParchment, style = MaterialTheme.typography.headlineLarge)
                }
                Column {
                    Text(place.title, color = PixelParchment, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Text("已留下 ${echoes.size} 天回响", color = PixelGold, style = MaterialTheme.typography.labelSmall)
                    if (place == WorldPlace.CAMP) {
                        val todayUnlocks = rewards.filter { it.sourceEntryId != null && it.sourceEntryId == todayEcho?.entry?.id }
                        todayUnlocks.firstOrNull()?.let { first ->
                            TextButton(onClick = {
                                if (todayUnlocks.size == 1) {
                                    place = first.reward.placement.place
                                    rewardDetailId = first.reward.id
                                } else {
                                    openPanel(CampPanel.FIRE)
                                }
                            }) {
                                Text(
                                    if (todayUnlocks.size == 1) "已解锁 · ${first.reward.title}"
                                    else "今日解锁 ${todayUnlocks.size} 件藏品",
                                    color = PixelGold,
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(place.hint, color = PixelGold, style = MaterialTheme.typography.labelMedium)
            when (place) {
                WorldPlace.CAMP -> {
                    TextButton(onClick = { openPanel(CampPanel.FIRE) }) {
                    Text(when {
                        todayEcho != null -> "查看今日回响"
                        status == ReviewStatus.FAILED -> "今日回响失败"
                        today != null -> "查看今日回响进度"
                        else -> "查看篝火"
                    }, color = PixelGold)
                    }
                    val pending = actionFollowups.count { it.status == "PENDING" && it.dueDate <= date.toString() }
                    TextButton(onClick = { openPanel(CampPanel.FOLLOWUPS) }) {
                        Text(if (pending > 0) "待回访 $pending 项" else "行动回访", color = PixelGold)
                    }
                }
                WorldPlace.WORKSHOP -> TextButton(onClick = { workshopTab = 0; openPanel(CampPanel.WORKBENCH) }) {
                    Text("查看项目木牌", color = PixelGold)
                }
                WorldPlace.PATH -> Row {
                    TextButton(onClick = { openPanel(CampPanel.PATH) }) { Text("旅程地图", color = PixelGold) }
                    if (echoes.size >= 7) {
                        TextButton(onClick = { openPanel(CampPanel.CHAPTER) }) { Text("首章回顾", color = PixelGold) }
                    }
                }
            }
            Text(message, Modifier.fillMaxWidth().background(Color(0xE8172831))
                .border(1.dp, Color(0xFF9B835C)).padding(horizontal = 13.dp, vertical = 11.dp),
                color = PixelParchment, style = MaterialTheme.typography.bodyMedium)
        }
        rewards.firstOrNull { it.reward.id == rewardDetailId }?.let { unlocked ->
            val reward = unlocked.reward
            val source = echoes.firstOrNull { it.entry.id == unlocked.sourceEntryId }
            val evidence = source?.evidence?.firstOrNull { it.attribute.growthTrack == reward.attribute }
            Box(Modifier.matchParentSize()) {
                Box(Modifier.matchParentSize().background(Color(0x99000000))
                    .clickable(onClickLabel = "关闭藏品详情") { rewardDetailId = null })
                Box(Modifier.align(Alignment.Center).padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())) {
                    ArtPanel(
                        art = R.drawable.world_tent_panel,
                        title = { Text(reward.title) },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Image(painterResource(reward.art), null, Modifier.size(112.dp))
                                Text("${reward.attribute.label} · ${reward.threshold} 点成长解锁", color = PixelGold)
                                if (evidence != null) Text("「${evidence.quote}」", color = PixelParchment)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                when (reward) {
                                    GrowthReward.BACKPACK -> {
                                        wearingPack = !wearingPack
                                        resting = false
                                    }
                                    GrowthReward.BENCH -> resting = !resting
                                    else -> Unit
                                }
                                rewardDetailId = null
                            }) {
                                Text(when (reward) {
                                    GrowthReward.BACKPACK -> if (wearingPack) "换回原装" else "装备背包"
                                    GrowthReward.BENCH -> if (resting) "起身" else "坐一会儿"
                                    else -> "留在这里"
                                })
                            }
                        },
                        dismissButton = if (evidence != null) {
                            {
                                TextButton(onClick = { rewardDetailId = null; detailId = source.entry.id }) {
                                    Text("查看旅志")
                                }
                            }
                        } else null,
                    )
                }
            }

        }
    }
    when (panel) {
        CampPanel.FIRE -> {
            val echo = todayEcho ?: echoes.firstOrNull().takeIf { today == null }
            val failure = status == ReviewStatus.FAILED
            ArtDialog(
                onDismissRequest = { panel = null },
                art = R.drawable.world_fire_panel,
                title = { Text("篝火回响") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            echo != null -> {
                                Text(echo.entry.localDate.toString())
                                Text(echo.excerpts().first())
                                rewards.filter { it.sourceEntryId == echo.entry.id }.forEach { unlock ->
                                    TextButton(onClick = {
                                        panel = null
                                        place = unlock.reward.placement.place
                                        rewardDetailId = unlock.reward.id
                                    }) {
                                        Text("已解锁 ${unlock.reward.title} · 去看看", color = PixelGold)
                                    }
                                }
                            }
                            today == null -> Text("写下一笔旅志，给营地留下一段回忆。")
                            status == ReviewStatus.RUNNING -> Text("正在汇总今天的旅志。")
                            failure -> Text(reviewFailures[today.id].orEmpty().ifBlank { "今日回响未完成，可以重试。" })
                            else -> Text("旅志已保存，等待回响。")
                        }
                    }
                },
                confirmButton = {
                    when {
                        echo != null -> TextButton(onClick = {
                            panel = null
                            detailId = echo.entry.id
                        }) { Text("查看完整旅志") }
                        today != null && status != ReviewStatus.RUNNING -> TextButton(onClick = {
                            if (aiConfigured) onRetry(today.id) else onOpenSettings()
                        }) { Text(if (!aiConfigured) "前往设置" else if (failure) "重试" else "现在汇总") }
                    }
                },
                dismissButton = { TextButton(onClick = { panel = null }) { Text("返回") } },
            )
        }
        CampPanel.FOLLOWUPS -> ActionFollowupDialog(
            today = date, projects = projects, followups = actionFollowups,
            onCreate = onCreateAction, onReschedule = onRescheduleAction,
            onComplete = onCompleteAction, onDismiss = { panel = null },
        )
        CampPanel.WORKBENCH -> ProjectWorkshopDialog(
            tab = workshopTab,
            projects = projects,
            milestones = projectMilestones,
            links = projectEntries,
            entries = entries,
            onOpenFollowups = { panel = CampPanel.FOLLOWUPS },
            onCreateProject = onCreateProject,
            onAddMilestone = onAddMilestone,
            onCompleteMilestone = onCompleteMilestone,
            onSetNextStep = onSetNextStep,
            onSetProjectEntry = onSetProjectEntry,
            onDismiss = { panel = null },
        )
        CampPanel.CHAPTER, CampPanel.PATH -> {
            val chapter = panel == CampPanel.CHAPTER
            val path = panel == CampPanel.PATH
            val displayed = validKeepsakes.firstOrNull { it.chapterSlot != null }
            val choices = if (chapter) {
                val chapterIds = firstChapter.map { it.entry.id }.toSet()
                validKeepsakes.filter { it.entryId in chapterIds }
            } else emptyList()
            val visibleDisplay = displayed?.takeIf { it in choices }
            ArtDialog(
                onDismissRequest = { if (!writing) panel = null },
                art = R.drawable.world_atlas_panel,
                title = { Text(if (chapter) "首章 · 最初七天" else "旅程地图") },
                text = {
                    LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (path || chapter) items(if (chapter) firstChapter else echoes, key = { it.entry.id }) { echo ->
                            TextButton(onClick = { detailId = echo.entry.id }) {
                                Text("${echo.entry.localDate} · ${echo.excerpts().first().take(28)}", maxLines = 2)
                            }
                        }
                        if (!path) {
                            item {
                                Text(when {
                                    visibleDisplay != null -> "当前展示 · ${visibleDisplay.title}"
                                    chapter -> "选一件源自这七天的纪念物，留作首章代表。"
                                    else -> "这段旅程还没有可展示的纪念物。"
                                })
                            }
                            if (visibleDisplay != null) item {
                                TextButton(onClick = { detailKeepsake = visibleDisplay.id }) { Text("查看纪念物原文") }
                                TextButton(enabled = !writing, onClick = {
                                    saveSelection({ onDisplay(null, chapter) }) {}
                                }) { Text("取下展示") }
                            }
                            items(choices, key = { "keepsake-${it.id}" }) { item ->
                                if (item.id != visibleDisplay?.id) TextButton(enabled = !writing, onClick = {
                                    saveSelection({ onDisplay(item.id, chapter) }) {
                                        message = "已选「${item.title}」作为首章代表。"
                                    }
                                }) { Text("${if (chapter) "选为代表" else "摆放"} · ${item.material} · ${item.title}") }
                            }
                            error?.let { problem -> item { Text(problem) } }
                        }
                    }
                },
                confirmButton = { TextButton(enabled = !writing, onClick = { panel = null }) { Text("返回") } },
            )
        }
        null -> Unit
    }

    val detailItem = validKeepsakes.firstOrNull { it.id == detailKeepsake }
    val detail = (detailItem?.entryId ?: detailId)?.let(journal::get)
    if (detail == null && (detailKeepsake != null || detailId != null)) {
        LaunchedEffect(detailKeepsake, detailId) {
            detailKeepsake = null
            detailId = null
        }
    }
    if (detail != null) {
        val dismissDetail = {
            detailId = null
            detailKeepsake = null
        }
        val echo = echoes.firstOrNull { it.entry.id == detail.id }
        ArtDialog(
            onDismissRequest = dismissDetail,
            art = R.drawable.world_atlas_panel,
            title = { Text(detailItem?.title ?: detail.localDate.toString()) },
            text = {
                LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (detailItem != null) item { Text("${detail.localDate} · ${detailItem.quote}") }
                    if (echo != null && echo.summary.isNotBlank()) item { Text(echo.summary) }
                    item { Text(detail.body) }
                }
            },
            confirmButton = { TextButton(onClick = dismissDetail) { Text("返回") } },
        )
    }
}

private fun GrowthSource.excerpts(): List<String> =
    evidence.map { it.quote }.filter { it.isNotBlank() && it in entry.body }.distinct()
        .ifEmpty { listOf(entry.body.trim().take(100)) }

@Composable
private fun ProjectWorkshopDialog(
    tab: Int,
    projects: List<ProjectEntity>,
    milestones: List<ProjectMilestoneEntity>,
    links: List<ProjectEntryEntity>,
    entries: List<DailyEntry>,
    onOpenFollowups: () -> Unit,
    onCreateProject: suspend (String) -> Long?,
    onAddMilestone: suspend (Long, String) -> Boolean,
    onCompleteMilestone: suspend (Long, Boolean) -> Boolean,
    onSetNextStep: suspend (Long, String) -> Boolean,
    onSetProjectEntry: suspend (Long, Long, Boolean) -> Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var projectId by remember { mutableStateOf<Long?>(null) }
    var name by remember { mutableStateOf("") }
    var milestoneTitle by remember { mutableStateOf("") }
    var nextStep by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val project = projects.firstOrNull { it.id == projectId }
    fun submit(action: suspend () -> Boolean, done: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                if (action()) done() else error = "保存失败，请检查内容后重试。"
            } catch (_: SQLiteException) {
                error = "保存失败，请重试。"
            } finally {
                busy = false
            }
        }
    }
    val panelName = listOf("木牌 · 项目", "纸页 · 旅志归档", "布片 · 里程碑")[tab.coerceIn(0, 2)]
    val paper = tab == 1
    val ink = if (paper) Color(0xFF273347) else PixelParchment
    val accent = if (paper) Color(0xFF405477) else PixelGold
    val art = when (tab) {
        0 -> R.drawable.world_project_board
        1 -> R.drawable.world_journal_page
        else -> R.drawable.world_milestone_banner
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(.82f).widthIn(max = 400.dp)) {
            Image(painterResource(art), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(panelName, color = accent, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(when (tab) {
                        0 -> "先立一个长期项目，再写下眼前的下一步。"
                        1 -> "选择项目，把相关旅志收进它的记录中。"
                        else -> "选择项目，写下阶段目标并记录完成时间。"
                    }, color = ink)
                }
                if (tab == 0) {
                    item {
                        WorkshopWritingFrame("新项目", "这段旅程想推进什么？", Color(0xFF6F5134)) {
                            OutlinedTextField(name, { name = it.take(80) },
                                label = { Text("项目名称") }, singleLine = true, enabled = !busy,
                                colors = workshopFieldColors(Color(0xFF472C1F)))
                            WorkshopButton(tab, "立起木牌", enabled = !busy && name.isNotBlank(), onClick = {
                                var createdId: Long? = null
                                submit({ onCreateProject(name).also { createdId = it } != null }) {
                                    projectId = createdId
                                    name = ""
                                    nextStep = ""
                                }
                            })
                        }
                    }
                }
                item { Text("所属项目", color = accent) }
                if (projects.isEmpty() && tab != 0) item {
                    Text("先轻触木牌创建一个项目。", color = ink)
                }
                items(projects, key = { "project-${it.id}" }) { item ->
                    WorkshopButton(tab, "${if (projectId == item.id) "●" else "○"} ${item.name}",
                        enabled = !busy, onClick = {
                            projectId = item.id
                            nextStep = item.nextStep
                        })
                }
                if (project != null) when (tab) {
                    0 -> item {
                        WorkshopWritingFrame("木牌上的下一步", "属于「${project.name}」", Color(0xFF6F5134)) {
                            OutlinedTextField(nextStep, { nextStep = it.take(240) },
                                label = { Text("接下来做什么") }, enabled = !busy,
                                colors = workshopFieldColors(Color(0xFF472C1F)))
                            WorkshopButton(tab, "刻下下一步", enabled = !busy, onClick = {
                                submit({ onSetNextStep(project.id, nextStep) })
                            })
                            if (project.nextStep.isNotBlank()) WorkshopButton(tab, "为这一步约定回访", onClick = onOpenFollowups)
                        }
                    }
                    1 -> {
                        item { Text("「${project.name}」的旅志", color = accent) }
                        if (entries.isEmpty()) item { Text("先写下一笔旅志。") }
                        items(entries.sortedByDescending { it.localDate }, key = { "entry-${it.id}" }) { entry ->
                            val linked = links.any { it.projectId == project.id && it.entryId == entry.id }
                            Box(Modifier.fillMaxWidth().background(Color(0xFF273D43))
                                .border(1.dp, Color(0xFF668A91)).padding(8.dp)) {
                                Column {
                                    Text(entry.localDate.toString(), color = PixelGold)
                                    Text(entry.body.take(100), color = PixelParchment, maxLines = 3)
                                    WorkshopButton(tab, if (linked) "移出项目" else "归入项目",
                                        enabled = !busy, onClick = {
                                            submit({ onSetProjectEntry(project.id, entry.id, !linked) })
                                        })
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            WorkshopWritingFrame("新的里程碑", "属于「${project.name}」", Color(0xFF5A485E)) {
                                OutlinedTextField(milestoneTitle, { milestoneTitle = it.take(100) },
                                    label = { Text("阶段目标") }, singleLine = true, enabled = !busy,
                                    colors = workshopFieldColors(Color(0xFF382442)))
                                WorkshopButton(tab, "缝上目标",
                                    enabled = !busy && milestoneTitle.isNotBlank(), onClick = {
                                        submit({ onAddMilestone(project.id, milestoneTitle) }) { milestoneTitle = "" }
                                    })
                            }
                        }
                        items(milestones.filter { it.projectId == project.id }, key = { "milestone-${it.id}" }) { item ->
                            val complete = item.completedAt != null
                            Box(Modifier.fillMaxWidth().background(Color(0xFF342B3C))
                                .border(1.dp, Color(0xFF8F7394)).padding(8.dp)) {
                                Column {
                                    Text(item.title, color = PixelParchment)
                                    WorkshopButton(tab, if (complete) "✓ 已完成 · 撤销" else "○ 标记完成",
                                        enabled = !busy, onClick = {
                                            submit({ onCompleteMilestone(item.id, !complete) })
                                        })
                                }
                            }
                        }
                    }
                }
                error?.let { problem -> item { Text(problem, color = accent) } }
                }
                WorkshopButton(tab, "返回工坊", enabled = !busy, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun WorkshopButton(
    tab: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val art = when (tab) {
        0 -> R.drawable.world_wood_button
        1 -> R.drawable.world_paper_button
        else -> R.drawable.world_cloth_button
    }
    Box(
        Modifier.fillMaxWidth().height(50.dp)
            .alpha(if (enabled) 1f else .45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(art), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Text(label, color = if (tab == 1) Color(0xFF273347) else PixelParchment,
            fontWeight = FontWeight.Bold, maxLines = 1,
            modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun WorkshopWritingFrame(
    title: String,
    hint: String,
    frameColor: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(frameColor.copy(alpha = .3f))
            .border(2.dp, frameColor).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = {
            Text(title, color = PixelGold, fontWeight = FontWeight.Bold)
            Text(hint, color = PixelParchment, style = MaterialTheme.typography.labelMedium)
            content()
        },
    )
}

@Composable
private fun workshopFieldColors(ink: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PixelParchment,
    unfocusedTextColor = PixelParchment,
    focusedBorderColor = PixelGold,
    unfocusedBorderColor = PixelParchment.copy(alpha = .65f),
    focusedLabelColor = PixelGold,
    unfocusedLabelColor = PixelParchment,
    cursorColor = PixelGold,
    focusedContainerColor = ink.copy(alpha = .4f),
    unfocusedContainerColor = ink.copy(alpha = .3f),
)

@Composable
private fun ActionFollowupDialog(
    today: LocalDate,
    projects: List<ProjectEntity>,
    followups: List<ActionFollowupEntity>,
    onCreate: suspend (Long, String, LocalDate) -> Boolean,
    onReschedule: suspend (Long, LocalDate) -> Boolean,
    onComplete: suspend (Long, String, String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var projectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var action by remember { mutableStateOf(projects.firstOrNull()?.nextStep.orEmpty()) }
    var dateText by remember { mutableStateOf(today.plusDays(1).toString()) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var outcome by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    fun submit(action: suspend () -> Boolean, done: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                if (action()) done() else error = "保存失败，请检查内容后重试。"
            } catch (_: SQLiteException) {
                error = "保存失败，请重试。"
            } finally {
                busy = false
            }
        }
    }
    ArtDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        art = R.drawable.world_tent_panel,
        title = { Text("行动回访") },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (projects.isEmpty()) item { Text("先到工坊创建一个项目。") }
                if (selected == null && projects.isNotEmpty()) {
                    item { Text("约定一个回看日期，再回来写下这次尝试的结果。") }
                    items(projects, key = { "project-${it.id}" }) { project ->
                        TextButton(onClick = { projectId = project.id; action = project.nextStep }) {
                            Text("${if (projectId == project.id) "●" else "○"} ${project.name}")
                        }
                    }
                    item {
                        OutlinedTextField(action, { action = it.take(240) },
                            label = { Text("准备尝试的行动") }, enabled = !busy)
                    }
                    item {
                        OutlinedTextField(dateText, { dateText = it },
                            label = { Text("回看日期 YYYY-MM-DD") }, singleLine = true, enabled = !busy)
                    }
                    item {
                        TextButton(enabled = !busy && action.isNotBlank() && date != null && projectId != null,
                            onClick = {
                                val id = projectId ?: return@TextButton
                                val due = date ?: return@TextButton
                                submit({ onCreate(id, action, due) }) { action = "" }
                            }) { Text("创建回访") }
                    }
                }
                if (selected != null) {
                    val item = followups.firstOrNull { it.id == selected }
                    if (item != null) {
                        item {
                            Text("${item.action} · 原定 ${item.dueDate}")
                            OutlinedTextField(outcome, { outcome = it.take(500) },
                                label = { Text("这次实际发生了什么") }, enabled = !busy)
                            OutlinedTextField(dateText, { dateText = it },
                                label = { Text("改约日期 YYYY-MM-DD") }, enabled = !busy, singleLine = true)
                            TextButton(enabled = !busy && date != null, onClick = {
                                val due = date ?: return@TextButton
                                submit({ onReschedule(item.id, due) }) { selected = null }
                            }) { Text("改日期") }
                            listOf("DONE" to "已完成", "ADJUSTED" to "调整了", "DROPPED" to "决定放下")
                                .forEach { (status, label) ->
                                    TextButton(enabled = !busy && outcome.isNotBlank(), onClick = {
                                        submit({ onComplete(item.id, status, outcome) }) {
                                            selected = null
                                            outcome = ""
                                        }
                                    }) { Text(label) }
                                }
                        }
                    }
                } else {
                    items(followups.sortedWith(compareBy<ActionFollowupEntity> { it.status != "PENDING" }
                        .thenBy { it.dueDate }), key = { "followup-${it.id}" }) { item ->
                        val project = projects.firstOrNull { it.id == item.projectId }?.name.orEmpty()
                        if (item.status == "PENDING") {
                            TextButton(onClick = {
                                selected = item.id
                                dateText = item.dueDate
                            }) {
                                Text("${if (item.dueDate <= today.toString()) "待回访" else "约定"} · ${item.dueDate} · $project · ${item.action}",
                                    maxLines = 2)
                            }
                        } else {
                            Text("${item.dueDate} · $project · ${item.action} · ${item.outcome}")
                        }
                    }
                }
                error?.let { problem -> item { Text(problem) } }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (selected != null) selected = null else onDismiss()
            }) { Text(if (selected != null) "返回列表" else "关闭") }
        },
    )
}

/** Aligns a touch target to the same center-cropped source art used by animated layers. */
@Composable
private fun SceneHotspotFromArt(
    label: String,
    sourceBounds: Rect,
    artWidth: Float,
    artHeight: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    pulse: Float,
    showMarker: Boolean = true,
    onClick: () -> Unit,
) {
    val scale = maxOf(screenWidth.value / artWidth, screenHeight.value / artHeight)
    val left = (screenWidth.value - artWidth * scale) / 2f
    val top = (screenHeight.value - artHeight * scale) / 2f
    SceneHotspot(
        label = label,
        x = (left + sourceBounds.left * scale) / screenWidth.value,
        y = (top + sourceBounds.top * scale) / screenHeight.value,
        widthFraction = sourceBounds.width * scale / screenWidth.value,
        heightFraction = sourceBounds.height * scale / screenHeight.value,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        pulse = pulse,
        showMarker = showMarker,
        onClick = onClick,
    )
}

@Composable
private fun SceneHotspot(
    label: String, x: Float, y: Float, widthFraction: Float, heightFraction: Float,
    screenWidth: Dp, screenHeight: Dp, pulse: Float, selected: Boolean = false,
    showMarker: Boolean = true, onClick: () -> Unit,
) {
    Box(Modifier.offset(screenWidth * x, screenHeight * y)
        .size(screenWidth * widthFraction, screenHeight * heightFraction)
        .semantics { contentDescription = "点击$label" }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        if (showMarker) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(if (selected) 26.dp else 21.dp).scale(pulse)
                .border(if (selected) 2.dp else 1.dp, PixelGold, CircleShape)
                .background(Color(0xAA362A20), CircleShape), contentAlignment = Alignment.Center) {
                Text("✦", color = PixelGold, style = MaterialTheme.typography.labelSmall)
            }
            Text(label, Modifier.padding(top = 3.dp).background(Color(0xBB101B24))
                .padding(horizontal = 5.dp, vertical = 2.dp),
                color = PixelParchment, style = MaterialTheme.typography.labelSmall)
        }
    }
}


package com.lifetimere.app.ui.screens

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifetimere.app.R
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.ui.theme.PixelParchment

private enum class WorldPlace(val title: String, val hint: String, val art: Int) {
    CAMP("回响营地", "轻触帐篷、工坊、篝火或远方的小径", R.drawable.world_camp_unlit_art),
    TENT("旅人的帐篷", "轻触记忆架，把一段旅程留在这里", R.drawable.world_tent_art),
    WORKSHOP("微光工坊", "轻触桌上材料，再轻触制作台", R.drawable.world_workshop_art),
    PATH("远望小径", "轻触路上的石刻，翻看旅程", R.drawable.world_path_art),
}

/** 美术与交互演示。当前文字和纪念物只存在于页面内存，不读写用户旅志。 */
@Composable
fun WorldDemoScreen() {
    var place by remember { mutableStateOf(WorldPlace.CAMP) }
    var message by remember { mutableStateOf("旅人已经回到营地。点亮一处物件，看看它怎样回应。") }
    var selectedMaterial by remember { mutableStateOf("木牌") }
    var selectionTick by remember { mutableStateOf(0) }
    var craftingTick by remember { mutableStateOf(0) }
    var keepsake by remember { mutableStateOf(false) }
    var fireLit by remember { mutableStateOf(false) }
    var pathDay by remember { mutableStateOf(0) }
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
                WorldPlace.WORKSHOP -> WorkshopScene(selectedMaterial, selectionTick, craftingTick, keepsake)
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
                    SceneHotspot("帐篷", .02f, .36f, .35f, .27f, maxWidth, maxHeight, pulse) {
                        place = WorldPlace.TENT; message = "帐篷里有一处记忆架，等着你的纪念物。"
                    }
                    SceneHotspot("工坊", .62f, .30f, .37f, .32f, maxWidth, maxHeight, pulse) {
                        place = WorldPlace.WORKSHOP; message = "桌上有木牌、纸页和布片。选一种开始制作。"
                    }
                    SceneHotspotFromArt(
                        label = if (fireLit) "熄灭篝火" else "点燃篝火",
                        sourceBounds = Rect(380f, 985f, 680f, 1200f),
                        artWidth = 1024f,
                        artHeight = 1536f,
                        screenWidth = maxWidth,
                        screenHeight = maxHeight,
                        pulse = pulse,
                        showMarker = !fireLit,
                    ) {
                        fireLit = !fireLit
                        message = if (fireLit) "火焰跃起，火星缓缓升向夜空。再轻触篝火可以熄灭。" else "火光渐渐收拢，只剩下安静的木柴。"
                    }
                    SceneHotspot("小径", .35f, .25f, .30f, .22f, maxWidth, maxHeight, pulse) {
                        place = WorldPlace.PATH; message = "小径上的石刻对应走过的日子。"
                    }
                    Image(
                        painter = painterResource(R.drawable.world_traveler_art), contentDescription = "旅人，点击打招呼",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.offset(maxWidth * .72f, maxHeight * .70f + travelerBob.dp)
                            .size(maxWidth * .20f, maxHeight * .19f)
                            .clickable { message = "旅人回头向你挥手：这里还会继续记住你的旅程。" },
                    )
                }
                WorldPlace.TENT -> {
                    SceneHotspot(if (keepsake) "纪念物" else "记忆架", .42f, .16f, .52f, .38f, maxWidth, maxHeight, pulse) {
                        keepsake = true
                        message = "你把“$selectedMaterial · 今天的尝试”摆上记忆架。"
                    }
                    if (keepsake) {
                        Text("✦  $selectedMaterial · 今天的尝试", color = PixelParchment,
                            modifier = Modifier.offset(maxWidth * .48f, maxHeight * .42f)
                                .background(Color(0xCC38271B)).padding(7.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                WorldPlace.WORKSHOP -> {
                    SceneHotspot("木牌", .13f, .54f, .23f, .15f, maxWidth, maxHeight, pulse, selectedMaterial == "木牌", showMarker = false) {
                        selectedMaterial = "木牌"; selectionTick++; message = "选中了木牌。轻触制作台，留下这段旅程。"
                    }
                    SceneHotspot("纸页", .37f, .54f, .23f, .15f, maxWidth, maxHeight, pulse, selectedMaterial == "纸页", showMarker = false) {
                        selectedMaterial = "纸页"; selectionTick++; message = "选中了纸页。轻触制作台，留下这段旅程。"
                    }
                    SceneHotspot("布片", .60f, .54f, .24f, .15f, maxWidth, maxHeight, pulse, selectedMaterial == "布片", showMarker = false) {
                        selectedMaterial = "布片"; selectionTick++; message = "选中了布片。轻触制作台，留下这段旅程。"
                    }
                    SceneHotspot("制作台", .23f, .70f, .56f, .15f, maxWidth, maxHeight, pulse, showMarker = false) {
                        keepsake = true; craftingTick++; message = "“$selectedMaterial · 今天的尝试”完成了。去帐篷看看记忆架。"
                    }
                }
                WorldPlace.PATH -> {
                    listOf(
                        Triple(.61f, .31f, 1), Triple(.38f, .56f, 2), Triple(.32f, .75f, 3),
                    ).forEach { (x, y, day) ->
                        SceneHotspot("第 $day 日", x, y, .27f, .12f, maxWidth, maxHeight, pulse) {
                            pathDay = day
                            message = "第 $pathDay 日的石刻亮起。正式版本会在这里打开当天真实的旅志。"
                        }
                    }
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
                    Text("世界页 · 美术交互演示", color = PixelGold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(place.hint, color = PixelGold, style = MaterialTheme.typography.labelMedium)
            Text(message, Modifier.fillMaxWidth().background(Color(0xE8172831))
                .border(1.dp, Color(0xFF9B835C)).padding(horizontal = 13.dp, vertical = 11.dp),
                color = PixelParchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
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

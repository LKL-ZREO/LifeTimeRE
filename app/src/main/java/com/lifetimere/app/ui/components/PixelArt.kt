package com.lifetimere.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifetimere.app.ui.theme.PixelAmber
import com.lifetimere.app.ui.theme.PixelCoral
import com.lifetimere.app.ui.theme.PixelForest
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelInk
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.ui.theme.PixelOutline
import com.lifetimere.app.ui.theme.PixelPanel
import com.lifetimere.app.ui.theme.PixelPanelRaised
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal
import com.lifetimere.app.domain.CampStage
import kotlinx.coroutines.delay

@Composable
fun PixelCampScene(
    stage: CampStage,
    settledDates: Int = 0,
    isVisible: Boolean = true,
    firstEchoSequence: Boolean = false,
    onFirstEchoAnimationComplete: () -> Unit = {},
    hotspotsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCampfireClick: () -> Unit = {},
    onSignpostClick: () -> Unit = {},
    onWorkshopClick: () -> Unit = {},
) {
    var animationTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isVisible, firstEchoSequence) {
        if (isVisible && firstEchoSequence) {
            animationTick = 0
            repeat(12) {
                delay(320)
                animationTick = it + 1
            }
            onFirstEchoAnimationComplete()
        } else if (isVisible) {
            while (true) {
                delay(320)
                animationTick = (animationTick + 1) % 32
            }
        }
    }
    val frame = animationTick % 4

    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.15f).background(PixelNight)) {
      Canvas(
        Modifier.fillMaxSize().semantics {
            contentDescription = "像素风回响营地，当前状态为${stage.label}，已完成 $settledDates 个有效回顾日，旅人正在营地中活动。"
        },
      ) {
        val unit = size.width / 64f
        fun px(x: Int, y: Int, w: Int, h: Int, color: Color) {
            drawRect(color, Offset(x * unit, y * unit), Size(w * unit, h * unit))
        }
        fun tree(x: Int, y: Int, tone: Color) {
            px(x + 4, y + 8, 2, 5, Color(0xFF5A402B))
            px(x + 3, y + 1, 4, 2, tone)
            px(x + 2, y + 3, 6, 2, tone)
            px(x + 1, y + 5, 8, 2, tone)
            px(x, y + 7, 10, 3, tone)
            px(x + 3, y + 3, 2, 5, PixelTeal.copy(alpha = 0.36f))
        }

        px(0, 0, 64, 40, Color(0xFF152431))
        px(0, 7, 64, 5, Color(0xFF1B3040))
        px(0, 12, 64, 5, Color(0xFF203B46))

        px(8, 4, 1, 1, PixelParchment.copy(alpha = 0.7f))
        px(16, 7, 1, 1, PixelTeal)
        px(29, 3, 1, 1, PixelParchment.copy(alpha = 0.6f))
        px(43, 6, 1, 1, PixelParchment.copy(alpha = 0.8f))
        px(54, 3, 2, 1, PixelGold.copy(alpha = 0.75f))
        px(56, 4, 1, 1, PixelGold.copy(alpha = 0.45f))

        px(0, 16, 9, 4, Color(0xFF2C4851))
        px(7, 13, 10, 7, Color(0xFF2C4851))
        px(15, 16, 8, 4, Color(0xFF2C4851))
        px(21, 11, 12, 9, Color(0xFF35545A))
        px(31, 15, 10, 5, Color(0xFF35545A))
        px(39, 10, 13, 10, Color(0xFF2C4851))
        px(50, 14, 14, 6, Color(0xFF2C4851))

        px(0, 20, 64, 20, Color(0xFF1E3834))
        px(0, 25, 64, 15, Color(0xFF25463C))
        px(0, 40, 64, 16, Color(0xFF25463C))
        px(26, 25, 13, 15, Color(0xFF4B5642))
        px(29, 24, 8, 16, Color(0xFF56634B))
        px(33, 24, 3, 16, Color(0xFF6B6A4E))
        px(28, 40, 9, 16, Color(0xFF4B5642))
        px(31, 40, 5, 16, Color(0xFF56634B))
        px(33, 40, 2, 16, Color(0xFF6B6A4E))

        tree(1, 14, Color(0xFF285446))
        tree(10, 16, Color(0xFF31634F))
        tree(49, 15, Color(0xFF285446))
        tree(56, 17, Color(0xFF31634F))

        px(8, 30, 16, 6, Color(0xFF283F36))
        px(11, 26, 10, 2, PixelParchment)
        px(9, 28, 14, 8, Color(0xFFC7A66A))
        px(12, 25, 8, 2, Color(0xFFE0C185))
        px(12, 29, 8, 7, Color(0xFFB38A56))
        px(15, 30, 3, 6, PixelInk)
        px(14, 31, 1, 5, Color(0xFF5A4633))

        val workshopWall = if (stage == CampStage.AWAKENED) Color(0xFF9A7040) else Color(0xFF815F38)
        px(45, 27, 10, 8, Color(0xFF19312E))
        px(46, 25, 8, 2, if (stage == CampStage.AWAKENED) Color(0xFF8A673A) else Color(0xFF6A5032))
        px(47, 27, 6, 6, workshopWall)
        px(48, 28, 4, 5, Color(0xFF243C39))
        px(50, 29, 1, 2, if (stage == CampStage.AWAKENED) PixelGold else PixelInk)
        if (stage == CampStage.AWAKENED) {
            px(46, 24, 1, 1, PixelGold.copy(alpha = 0.65f))
            px(54, 29, 2, 1, PixelParchment.copy(alpha = 0.5f))
        }

        if (settledDates >= 2) {
            px(23, 37, 5, 4, Color(0xFF785A39))
            px(24, 36, 3, 1, PixelGold.copy(alpha = 0.8f))
            px(24, 38, 1, 2, Color(0xFFBA8C4D))
        }
        if (settledDates >= 5) {
            px(7, 34, 2, 8, Color(0xFF624A30))
            px(6, 32, 4, 3, Color(0xFFB07B3D))
            px(7, 33, 2, 1, PixelGold)
            px(5, 41, 5, 1, PixelGold.copy(alpha = 0.45f))
        }
        if (settledDates >= 7) {
            px(3, 24, 1, 13, Color(0xFF60472C))
            px(4, 25, 6, 3, Color(0xFFB14F43))
            px(4, 28, 4, 2, PixelGold)
            px(5, 29, 1, 1, PixelParchment)
        }

        px(40, 27, 1, 8, Color(0xFF60472C))
        val signColor = if (stage == CampStage.DORMANT) Color(0xFF5F4C36) else Color(0xFFB07B3D)
        px(38, 28, 5, 2, signColor)
        px(38, 31, 5, 2, signColor)
        px(39, 28, 1, 1, if (stage == CampStage.DORMANT) PixelMuted.copy(alpha = 0.35f) else PixelParchment)

        val fireStrength = if (stage == CampStage.DORMANT) 0.07f else 0.2f
        px(27, 32, 11, 3, PixelGold.copy(alpha = fireStrength))
        px(29, 31, 7, 4, PixelGold.copy(alpha = fireStrength + 0.05f))
        px(30, 34, 6, 2, Color(0xFF69513A))
        px(31, 33, 4, 1, Color(0xFF2A1D17))
        if (stage == CampStage.DORMANT) {
            px(32, 31, 2, 2, PixelCoral.copy(alpha = 0.7f))
            px(33, 30, 1, 2, PixelGold.copy(alpha = 0.65f))
        } else if (frame % 2 == 0) {
            px(31, 29, 4, 4, PixelCoral)
            px(32, 27, 2, 4, PixelGold)
            px(32, 29, 1, 2, PixelParchment)
        } else {
            px(31, 30, 4, 3, PixelCoral)
            px(32, 28, 2, 4, PixelAmber)
            px(33, 26, 1, 3, PixelGold)
        }

        val fireflies = listOf(25 to 25, 42 to 22, 55 to 25, 6 to 24)
        fireflies.forEachIndexed { index, (x, y) ->
            val offset = if ((frame + index) % 3 == 0) 1 else 0
            px(x, y - offset, 1, 1, PixelGold.copy(alpha = 0.45f + index * 0.1f))
        }

        for (x in 2 until 62 step 6) {
            px(x, 49 - (x % 3), 1, 1, PixelForest.copy(alpha = 0.7f))
        }
        px(0, 55, 64, 1, PixelInk.copy(alpha = 0.45f))

        val travelPhase = animationTick % 32
        val travelerX = when (travelPhase) {
            in 0..7 -> 22 + travelPhase
            in 8..13 -> 30
            in 14..21 -> 30 + (travelPhase - 14) * 2
            in 22..27 -> 46
            else -> 46 - (travelPhase - 27) * 6
        }
        val walking = travelPhase in 0..7 || travelPhase in 14..21 || travelPhase >= 28
        val legOffset = if (walking && frame % 2 == 0) 1 else 0
        val travelerY = if (travelPhase in 8..13 || travelPhase in 22..27) 40 else 41
        px(travelerX, travelerY + 10, 6, 1, PixelInk.copy(alpha = 0.65f))
        px(travelerX + 1, travelerY + 7, 2, 4, Color(0xFF5A4633))
        px(travelerX + 4, travelerY + 7 + legOffset, 2, 4 - legOffset, Color(0xFF49382D))
        px(travelerX, travelerY + 4, 6, 4, Color(0xFF376E62))
        px(travelerX - 1, travelerY + 5, 2, 4, Color(0xFF2B594F))
        px(travelerX + 5, travelerY + 5, 2, 4, Color(0xFF2B594F))
        px(travelerX + 1, travelerY, 4, 4, Color(0xFFD6AE72))
        px(travelerX, travelerY + 1, 1, 3, Color(0xFF5D4431))
        px(travelerX + 5, travelerY + 1, 1, 3, Color(0xFF5D4431))
        px(travelerX + 1, travelerY - 1, 4, 2, Color(0xFF5D4431))
        px(travelerX + 2, travelerY + 2, 1, 1, PixelInk)
        px(travelerX + 4, travelerY + 2, 1, 1, PixelInk)
        px(travelerX + 5, travelerY + 5, 2, 3, Color(0xFF765438))
      }

      CampHotspot(
          description = if (stage == CampStage.DORMANT) "尚未点亮的篝火，查看营地档案" else "点亮的篝火，查看营地等级和来源旅志",
          enabled = hotspotsEnabled,
          onClick = onCampfireClick,
          modifier = Modifier.offset(maxWidth * 0.40f, maxHeight * 0.45f).size(maxWidth * 0.20f, maxHeight * 0.20f),
      )
      CampHotspot(
          description = if (stage == CampStage.DORMANT) "沉睡的旧路标，查看旅程记录" else "已点亮的路标，查看来源旅志",
          enabled = hotspotsEnabled,
          onClick = onSignpostClick,
          modifier = Modifier.offset(maxWidth * 0.57f, maxHeight * 0.44f).size(maxWidth * 0.15f, maxHeight * 0.19f),
      )
      CampHotspot(
          description = if (stage == CampStage.AWAKENED) "已点亮的工坊，查看来源旅志" else "尚未苏醒的工坊，查看开启条件",
          enabled = hotspotsEnabled,
          onClick = onWorkshopClick,
          modifier = Modifier.offset(maxWidth * 0.69f, maxHeight * 0.40f).size(maxWidth * 0.19f, maxHeight * 0.22f),
      )
    }
}

@Composable
private fun CampHotspot(description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

@Composable
fun PixelTraveler(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .aspectRatio(0.75f)
            .background(PixelPanelRaised)
            .border(2.dp, PixelOutline)
            .semantics { contentDescription = "像素旅人，披着青绿色斗篷，背着旅行包。" },
    ) {
        val unit = minOf(size.width / 24f, size.height / 32f)
        val ox = (size.width - unit * 24) / 2f
        val oy = (size.height - unit * 32) / 2f
        fun px(x: Int, y: Int, w: Int, h: Int, color: Color) {
            drawRect(color, Offset(ox + x * unit, oy + y * unit), Size(w * unit, h * unit))
        }

        px(3, 27, 18, 3, PixelInk.copy(alpha = 0.7f))
        px(6, 25, 5, 3, Color(0xFF3A2D26)); px(14, 25, 5, 3, Color(0xFF3A2D26))
        px(7, 21, 4, 5, Color(0xFFB58A55)); px(14, 21, 4, 5, Color(0xFFB58A55))
        px(5, 14, 15, 8, Color(0xFF376E62))
        px(3, 16, 4, 7, Color(0xFF2B594F)); px(18, 16, 3, 7, Color(0xFF2B594F))
        px(8, 14, 9, 8, Color(0xFFCCAA68))
        px(10, 17, 5, 2, PixelGold)
        px(9, 20, 7, 2, Color(0xFF725238))
        px(17, 15, 4, 6, Color(0xFF765438))
        px(18, 14, 3, 2, Color(0xFF95704A))
        px(20, 18, 2, 7, Color(0xFFB9C4B2))
        px(21, 16, 1, 4, PixelParchment)

        px(7, 6, 11, 8, Color(0xFFD6AE72))
        px(6, 7, 2, 6, Color(0xFF5D4431)); px(17, 7, 2, 6, Color(0xFF5D4431))
        px(8, 4, 9, 4, Color(0xFF5D4431))
        px(10, 3, 6, 2, Color(0xFF5D4431))
        px(10, 9, 2, 1, PixelInk); px(15, 9, 2, 1, PixelInk)
        px(12, 12, 3, 1, PixelCoral.copy(alpha = 0.75f))
        px(6, 13, 13, 3, PixelCoral)
        px(8, 14, 9, 2, Color(0xFFA64F43))
        px(4, 10, 2, 11, Color(0xFF244B45))
        px(3, 11, 2, 7, Color(0xFF315E52))
        px(4, 9, 2, 2, PixelGold)
    }
}

@Composable
fun PixelJournalIcon(size: Dp = 96.dp) {
    Canvas(Modifier.size(size).semantics { contentDescription = "一本等待书写的像素冒险日志。" }) {
        val unit = this.size.minDimension / 16f
        fun px(x: Int, y: Int, w: Int, h: Int, color: Color) {
            drawRect(color, Offset(x * unit, y * unit), Size(w * unit, h * unit))
        }
        px(2, 2, 12, 13, PixelInk)
        px(3, 1, 10, 13, Color(0xFF8B6940))
        px(5, 2, 8, 11, Color(0xFFD4B77A))
        px(4, 1, 2, 13, Color(0xFF5F4930))
        px(7, 4, 4, 1, PixelCoral)
        px(7, 7, 4, 1, Color(0xFF8C7652))
        px(7, 9, 3, 1, Color(0xFF8C7652))
        px(7, 11, 4, 1, Color(0xFF8C7652))
        px(2, 4, 2, 2, PixelGold)
    }
}

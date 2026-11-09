package com.lifetimere.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.imageResource
import com.lifetimere.app.R
import com.lifetimere.app.ui.components.PixelSceneViewport
import com.lifetimere.app.ui.components.drawPixelAtlasFrame
import com.lifetimere.app.ui.components.drawPixelScene
import com.lifetimere.app.ui.components.rememberPixelSpriteFrame

/** One source-pixel coordinate system for the backdrop, sprite, and foreground occlusion. */
@Composable
internal fun CampfireScene(lit: Boolean) {
    val backdrop = ImageBitmap.imageResource(R.drawable.world_camp_unlit_art)
    val flames = ImageBitmap.imageResource(R.drawable.world_fire_frames_art)
    val waterfallCleanPlate = ImageBitmap.imageResource(R.drawable.world_camp_waterfall_clean_art)
    val waterfallFrames = ImageBitmap.imageResource(R.drawable.world_waterfall_frames_art)
    val frame = rememberPixelSpriteFrame(lit, frameCount = 4, frameMillis = 155L)
    val waterfallFrame = rememberPixelSpriteFrame(true, frameCount = 4, frameMillis = 190L)
    val visibility by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(if (lit) 350 else 260),
        label = "篝火点燃与熄灭",
    )
    val motion = rememberInfiniteTransition(label = "篝火环境动画")
    val glow by motion.animateFloat(
        initialValue = .75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "篝火光照",
    )
    val emberTravel by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "火星轨迹",
    )
    val waterSpray by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "瀑布水花",
    )
    val lanternPulse by motion.animateFloat(
        initialValue = .75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "营地灯光",
    )
    val smokeTravel by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Restart),
        label = "烟囱炊烟",
    )

    Canvas(Modifier.fillMaxSize()) {
        val viewport = PixelSceneViewport.fit(size, backdrop)
        drawPixelScene(backdrop, viewport)
        drawWaterfallFrames(viewport, backdrop, waterfallCleanPlate, waterfallFrames, waterfallFrame)
        drawWaterfallSpray(viewport, waterSpray)
        drawLanternBreathing(viewport, lanternPulse)
        drawChimneySmoke(viewport, smokeTravel)
        if (visibility > 0f) {
            drawCampfireGlow(viewport, visibility * glow)
            val horizontalAdjustment = floatArrayOf(-5f, 5f, -4f, 4f)[frame]
            val verticalAdjustment = if (frame >= 2) 14f else 0f
            drawPixelAtlasFrame(
                atlas = flames,
                columns = 2,
                rows = 2,
                frame = frame,
                viewport = viewport,
                sourceX = 422f + horizontalAdjustment,
                sourceY = 940f + verticalAdjustment,
                sourceWidth = 240f,
                sourceHeight = 220f,
                alpha = visibility,
            )
            // Redraw unchanged source pixels over the flame wherever a real object sits in front.
            clipPath(campfireForeground(viewport)) { drawPixelScene(backdrop, viewport) }
            drawCampfireEmbers(viewport, visibility, emberTravel)
        }
    }
}

private fun DrawScope.drawCampfireGlow(viewport: PixelSceneViewport, strength: Float) {
    val center = viewport.point(512f, 1100f)
    val radius = viewport.pixels(126f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x77F6A23A).copy(alpha = .28f * strength), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawCampfireEmbers(
    viewport: PixelSceneViewport, visibility: Float, travel: Float,
) {
    val offsets = floatArrayOf(-31f, 18f, -8f, 33f, 3f, -20f)
    offsets.forEachIndexed { index, offset ->
        val phase = (travel + index * .18f) % 1f
        val origin = viewport.point(512f + offset, 1050f - phase * 125f)
        val side = viewport.pixels(if (index % 2 == 0) 2.5f else 1.5f)
        drawRect(
            color = Color(0xFFFFC65A).copy(alpha = visibility * (1f - phase)),
            topLeft = origin,
            size = Size(side, side),
        )
    }
}

private fun campfireForeground(viewport: PixelSceneViewport): Path = Path().apply {
    fun polygon(vararg points: Pair<Float, Float>) {
        points.forEachIndexed { index, (x, y) ->
            val mapped = viewport.point(x, y)
            if (index == 0) moveTo(mapped.x, mapped.y) else lineTo(mapped.x, mapped.y)
        }
        close()
    }

    // The hanging pot and both support poles are in front of the fire.
    val potTopLeft = viewport.point(454f, 955f)
    val potBottomRight = viewport.point(560f, 1050f)
    addOval(Rect(potTopLeft, potBottomRight))

}

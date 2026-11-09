package com.lifetimere.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import com.lifetimere.app.R
import com.lifetimere.app.ui.components.PixelSceneViewport
import com.lifetimere.app.ui.components.drawPixelScene
import com.lifetimere.app.ui.components.rememberPixelSpriteFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Full-scene workshop animation with short reactions tied to the existing hotspots. */
private const val WORKSHOP_DISPLAY_STEP_MS = 45L
private const val WORKSHOP_BLEND_STEPS = 5
private const val WORKSHOP_SEGMENTS = 16

@Composable
internal fun WorkshopScene(
    selectedMaterial: String,
    selectionTick: Int,
    craftingTick: Int,
    completed: Boolean,
) {
    val backdrops = listOf(
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_01),
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_02),
        ImageBitmap.imageResource(R.drawable.world_workshop_frame_1),
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_03),
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_04),
        ImageBitmap.imageResource(R.drawable.world_workshop_frame_2),
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_05),
        ImageBitmap.imageResource(R.drawable.world_workshop_bridge_06),
        ImageBitmap.imageResource(R.drawable.world_workshop_frame_3),
    )
    // Nine authored scene frames make the motion; short blends only soften residual texture drift.
    val frame = rememberPixelSpriteFrame(
        playing = true,
        frameCount = WORKSHOP_BLEND_STEPS * WORKSHOP_SEGMENTS,
        frameMillis = WORKSHOP_DISPLAY_STEP_MS,
    )
    val segment = frame / WORKSHOP_BLEND_STEPS
    val blend = when (frame % WORKSHOP_BLEND_STEPS) {
        0, 1, 2 -> 0f
        3 -> .45f
        else -> .85f
    }
    val backdrop = backdrops[workshopFrameSequence[segment]]
    val nextBackdrop = backdrops[workshopFrameSequence[segment + 1]]
    val cycle = frame.toFloat() / (WORKSHOP_BLEND_STEPS * WORKSHOP_SEGMENTS)
    val firelight = .80f + .20f * sin(cycle * PI * 8).toFloat()
    val lamplight = .87f + .13f * sin(cycle * PI * 2).toFloat()
    val dustTravel = cycle
    val furnaceEmbers = (frame % 27) / 27f
    val selectionProgress = remember { Animatable(1f) }
    val craftingProgress = remember { Animatable(1f) }
    LaunchedEffect(selectionTick) {
        if (selectionTick > 0) {
            selectionProgress.snapTo(0f)
            selectionProgress.animateTo(1f, tween(650))
        }
    }
    LaunchedEffect(craftingTick) {
        if (craftingTick > 0) {
            craftingProgress.snapTo(0f)
            craftingProgress.animateTo(1f, tween(1050))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val viewport = PixelSceneViewport.fit(size, backdrop)
        drawPixelScene(backdrop, viewport)
        drawPixelScene(nextBackdrop, viewport, alpha = blend)
        drawWorkshopLight(viewport, firelight, lamplight)
        drawWorkshopDust(viewport, dustTravel)
        drawFurnaceEmbers(viewport, furnaceEmbers)
        val material = materialBounds(selectedMaterial)
        drawMaterialFocus(viewport, material, lamplight, selectionProgress.value, completed)
        if (craftingProgress.value < 1f) {
            drawCraftingBurst(viewport, material.center, craftingProgress.value)
        }
    }
}

private val workshopFrameSequence = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 1, 0)

private fun DrawScope.drawFurnaceEmbers(viewport: PixelSceneViewport, travel: Float) {
    repeat(8) { index ->
        val phase = (travel + index * .16f) % 1f
        val point = viewport.point(
            891f + (index % 4) * 18f + phase * 7f,
            561f - phase * 87f,
        )
        val side = viewport.pixels(if (index % 3 == 0) 3f else 2f)
        drawRect(
            color = Color(0xFFFFD36D).copy(alpha = .82f * (1f - phase)),
            topLeft = point,
            size = Size(side, side),
        )
    }
}

private fun materialBounds(material: String): Rect = when (material) {
    "纸页" -> Rect(365f, 865f, 545f, 1000f)
    "布片" -> Rect(550f, 872f, 715f, 1015f)
    else -> Rect(208f, 883f, 350f, 987f)
}

private fun DrawScope.drawWorkshopLight(viewport: PixelSceneViewport, fire: Float, lamp: Float) {
    listOf(
        Triple(905f, 550f, 120f) to (.20f * fire),
        Triple(325f, 205f, 110f) to (.13f * lamp),
        Triple(745f, 842f, 86f) to (.15f * lamp),
    ).forEach { (position, strength) ->
        val (x, y, radiusInArtPixels) = position
        val center = viewport.point(x, y)
        val radius = viewport.pixels(radiusInArtPixels)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFB94D).copy(alpha = strength), Color.Transparent),
                center = center,
                radius = radius,
            ),
            center = center,
            radius = radius,
        )
    }
}

private fun DrawScope.drawWorkshopDust(viewport: PixelSceneViewport, travel: Float) {
    repeat(9) { index ->
        val phase = (travel + index * .13f) % 1f
        val point = viewport.point(
            445f + (index * 31f) % 280f + phase * 9f,
            520f - phase * 185f + (index % 3) * 12f,
        )
        val side = viewport.pixels(if (index % 3 == 0) 2f else 1f)
        drawRect(
            color = Color(0xFFFFE7AC).copy(alpha = .42f * (1f - phase)),
            topLeft = point,
            size = Size(side, side),
        )
    }
}

private fun DrawScope.drawMaterialFocus(
    viewport: PixelSceneViewport,
    bounds: Rect,
    pulse: Float,
    selectionProgress: Float,
    completed: Boolean,
) {
    val center = viewport.point(bounds.center.x, bounds.center.y)
    val radius = viewport.pixels(72f + (1f - selectionProgress) * 18f)
    if (selectionProgress < 1f) {
        drawRect(
            color = Color(0xFFFFD783).copy(alpha = .26f * (1f - selectionProgress)),
            topLeft = viewport.point(bounds.left, bounds.top),
            size = Size(viewport.pixels(bounds.width), viewport.pixels(bounds.height)),
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD46E).copy(alpha = (.10f + if (completed) .08f else 0f) * pulse),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        center = center,
        radius = radius,
    )
    val cornerLength = viewport.pixels(15f)
    val lineWidth = viewport.pixels(3f)
    val accent = Color(0xFFE9BF68).copy(alpha = .75f + .25f * (1f - selectionProgress))
    listOf(bounds.topLeft, Offset(bounds.right, bounds.top), Offset(bounds.left, bounds.bottom), bounds.bottomRight)
        .forEachIndexed { index, corner ->
            val mapped = viewport.point(corner.x, corner.y)
            val horizontalX = if (index % 2 == 0) mapped.x else mapped.x - cornerLength
            val verticalY = if (index < 2) mapped.y else mapped.y - cornerLength
            drawRect(accent, Offset(horizontalX, mapped.y), Size(cornerLength, lineWidth))
            drawRect(accent, Offset(mapped.x, verticalY), Size(lineWidth, cornerLength))
        }
}

private fun DrawScope.drawCraftingBurst(
    viewport: PixelSceneViewport,
    materialCenter: Offset,
    progress: Float,
) {
    drawRect(Color(0xFFFFD783).copy(alpha = .18f * (1f - progress)))
    val center = viewport.point(materialCenter.x, materialCenter.y)
    val radius = viewport.pixels(40f + progress * 145f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD37A).copy(alpha = .78f * (1f - progress)), Color.Transparent),
            center = center,
            radius = radius,
        ),
        center = center,
        radius = radius,
    )
    repeat(12) { index ->
        val angle = index * (PI * 2 / 12)
        val distance = viewport.pixels(12f + progress * 105f)
        val point = Offset(
            center.x + cos(angle).toFloat() * distance,
            center.y + sin(angle).toFloat() * distance * .55f,
        )
        val side = viewport.pixels(if (index % 3 == 0) 6f else 3f)
        drawRect(
            color = Color(0xFFFFD15C).copy(alpha = 1f - progress),
            topLeft = point,
            size = Size(side, side),
        )
    }
}

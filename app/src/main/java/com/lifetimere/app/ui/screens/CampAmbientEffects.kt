package com.lifetimere.app.ui.screens

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.lifetimere.app.ui.components.PixelSceneViewport
import com.lifetimere.app.ui.components.drawPixelAtlasFrame
import com.lifetimere.app.ui.components.drawPixelScene

/** Replaces just the painted waterfall with a clean cliff and a four-frame water silhouette. */
internal fun DrawScope.drawWaterfallFrames(
    viewport: PixelSceneViewport,
    original: ImageBitmap,
    cleanPlate: ImageBitmap,
    frames: ImageBitmap,
    frame: Int,
) {
    clipPath(waterfallChannel(viewport)) {
        drawPixelScene(cleanPlate, viewport)
    }
    drawPixelAtlasFrame(
        atlas = frames,
        columns = 2,
        rows = 2,
        frame = frame,
        viewport = viewport,
        sourceX = 80f + if (frame % 2 == 1) 8f else 0f,
        sourceY = 365f,
        sourceWidth = 300f,
        sourceHeight = 250f,
    )
    // These near cliff edges remain in front of the moving sheet.
    clipPath(waterfallForeground(viewport)) { drawPixelScene(original, viewport) }
}

internal fun DrawScope.drawWaterfallSpray(viewport: PixelSceneViewport, spray: Float) {
    val foamXs = floatArrayOf(211f, 231f, 249f, 269f, 289f, 308f)
    foamXs.forEachIndexed { index, x ->
        val phase = (spray + index * .23f) % 1f
        val origin = viewport.point(x + phase * 9f, 539f - phase * 16f)
        drawRect(
            color = Color(0xFFD8F6FD).copy(alpha = .65f * (1f - phase)),
            topLeft = origin,
            size = viewport.size(if (index % 2 == 0) 3f else 2f, 3f),
        )
    }
    // Ripples appear at different times so the pool does not pulse as one rigid shape.
    repeat(3) { index ->
        val phase = (spray + index / 3f) % 1f
        val origin = viewport.point(244f + index * 24f - phase * 8f, 558f + index * 4f)
        drawRect(
            color = Color(0xFFB8EAF7).copy(alpha = .3f * (1f - phase)),
            topLeft = origin,
            size = viewport.size(12f + phase * 13f, 2f),
        )
    }
}

internal fun DrawScope.drawLanternBreathing(viewport: PixelSceneViewport, strength: Float) {
    listOf(
        Triple(209f, 633f, 58f),
        Triple(742f, 656f, 49f),
    ).forEach { (x, y, radiusInArtPixels) ->
        val center = viewport.point(x, y)
        val radius = viewport.pixels(radiusInArtPixels)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF6BC5B).copy(alpha = .11f * strength), Color.Transparent),
                center = center,
                radius = radius,
            ),
            center = center,
            radius = radius,
        )
    }
}

internal fun DrawScope.drawChimneySmoke(viewport: PixelSceneViewport, travel: Float) {
    repeat(4) { index ->
        val phase = (travel + index * .27f) % 1f
        val origin = viewport.point(958f - phase * 18f + index * 3f, 286f - phase * 92f)
        val side = viewport.pixels(5f + phase * 7f)
        drawRect(
            color = Color(0xFFD6DCE5).copy(alpha = .17f * (1f - phase)),
            topLeft = origin,
            size = Size(side, side * .65f),
        )
    }
}

private fun waterfallChannel(viewport: PixelSceneViewport): Path = Path().apply {
    val points = listOf(
        122f to 375f, 284f to 375f, 288f to 439f, 274f to 475f,
        302f to 499f, 333f to 542f, 318f to 572f, 252f to 559f,
        224f to 519f, 183f to 499f, 139f to 471f, 119f to 435f,
    )
    points.forEachIndexed { index, (x, y) ->
        val mapped = viewport.point(x, y)
        if (index == 0) moveTo(mapped.x, mapped.y) else lineTo(mapped.x, mapped.y)
    }
    close()
}

private fun waterfallForeground(viewport: PixelSceneViewport): Path = Path().apply {
    val rockEdges = listOf(
        listOf(87f to 360f, 130f to 360f, 128f to 435f, 112f to 477f, 87f to 477f),
        listOf(284f to 361f, 355f to 361f, 355f to 490f, 309f to 490f, 283f to 449f),
        // The tent roof sits in front of the lower cascade in this perspective.
        listOf(0f to 575f, 216f to 543f, 424f to 770f, 0f to 813f),
    )
    rockEdges.forEach { points ->
        points.forEachIndexed { index, (x, y) ->
            val mapped = viewport.point(x, y)
            if (index == 0) moveTo(mapped.x, mapped.y) else lineTo(mapped.x, mapped.y)
        }
        close()
    }
}

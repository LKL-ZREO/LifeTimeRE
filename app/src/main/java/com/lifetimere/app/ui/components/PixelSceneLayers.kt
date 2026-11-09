package com.lifetimere.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/** Maps source-art pixels to a portrait canvas using the same center-crop for every layer. */
class PixelSceneViewport private constructor(
    private val scale: Float,
    private val left: Float,
    private val top: Float,
) {
    fun point(x: Float, y: Float): Offset = Offset(left + x * scale, top + y * scale)
    fun size(width: Float, height: Float): Size = Size(width * scale, height * scale)
    fun pixels(value: Float): Float = value * scale

    companion object {
        fun fit(canvas: Size, image: ImageBitmap): PixelSceneViewport {
            val scale = max(canvas.width / image.width, canvas.height / image.height)
            return PixelSceneViewport(
                scale,
                (canvas.width - image.width * scale) / 2f,
                (canvas.height - image.height * scale) / 2f,
            )
        }
    }
}

fun DrawScope.drawPixelScene(image: ImageBitmap, viewport: PixelSceneViewport, alpha: Float = 1f) {
    val origin = viewport.point(0f, 0f)
    val dimensions = viewport.size(image.width.toFloat(), image.height.toFloat())
    drawImage(
        image = image,
        dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
        dstSize = IntSize(dimensions.width.roundToInt(), dimensions.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}

fun DrawScope.drawPixelAtlasFrame(
    atlas: ImageBitmap,
    columns: Int,
    rows: Int,
    frame: Int,
    viewport: PixelSceneViewport,
    sourceX: Float,
    sourceY: Float,
    sourceWidth: Float,
    sourceHeight: Float,
    alpha: Float = 1f,
) {
    require(columns > 0 && rows > 0 && frame in 0 until columns * rows)
    val cellWidth = atlas.width / columns
    val cellHeight = atlas.height / rows
    val origin = viewport.point(sourceX, sourceY)
    val dimensions = viewport.size(sourceWidth, sourceHeight)
    drawImage(
        image = atlas,
        srcOffset = IntOffset((frame % columns) * cellWidth, (frame / columns) * cellHeight),
        srcSize = IntSize(cellWidth, cellHeight),
        dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
        dstSize = IntSize(dimensions.width.roundToInt(), dimensions.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}

/** Shared frame clock for four-frame or larger pixel-art sprite atlases. */
@Composable
fun rememberPixelSpriteFrame(playing: Boolean, frameCount: Int, frameMillis: Long): Int {
    var frame by remember(frameCount) { mutableIntStateOf(0) }
    LaunchedEffect(playing, frameCount, frameMillis) {
        frame = 0
        if (playing && frameCount > 1) {
            while (true) {
                delay(frameMillis)
                frame = (frame + 1) % frameCount
            }
        }
    }
    return frame
}

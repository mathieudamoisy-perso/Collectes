package com.smirtom.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.smirtom.app.R
import kotlin.math.roundToInt

/**
 * Crop of [R.drawable.ic_launcher_fg] (1024×1024) around the truck silhouette,
 * so the pull-to-refresh animation matches the app icon exactly.
 */
private val TruckSrcOffset = IntOffset(250, 350)
private val TruckSrcSize = IntSize(520, 320)

@Composable
fun GarbageTruckCanvas(
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 40.dp,
    animate: Boolean = false,
    pullProgress: Float = 1f,
) {
    val infinite = rememberInfiniteTransition(label = "truckCanvas")
    val driveOffset by infinite.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drive"
    )
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -2f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val progress = pullProgress.coerceIn(0f, 1f)
    val offsetX = if (animate) driveOffset else 0f
    val offsetY = if (animate) bounce else 0f
    val scaleFactor = if (animate) 1f else 0.75f + progress * 0.25f

    val truckBitmap: ImageBitmap = ImageBitmap.imageResource(R.drawable.ic_launcher_fg)

    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val srcAspect = TruckSrcSize.width.toFloat() / TruckSrcSize.height
        val canvasAspect = this.size.width / this.size.height
        val drawW: Float
        val drawH: Float
        if (srcAspect > canvasAspect) {
            drawW = this.size.width
            drawH = this.size.width / srcAspect
        } else {
            drawH = this.size.height
            drawW = this.size.height * srcAspect
        }
        val dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1))
        val dstOffset = IntOffset(-dstSize.width / 2, -dstSize.height / 2)

        translate(centerX + offsetX, centerY + offsetY) {
            scale(scaleFactor, pivot = Offset.Zero) {
                drawImage(
                    image = truckBitmap,
                    srcOffset = TruckSrcOffset,
                    srcSize = TruckSrcSize,
                    dstOffset = dstOffset,
                    dstSize = dstSize
                )
            }
        }
    }
}

@Composable
fun TruckRoadCanvas(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
) {
    val infinite = rememberInfiniteTransition(label = "road")
    val scroll by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roadScroll"
    )

    Canvas(modifier = modifier) {
        val dashWidth = size.width / 4f
        val offset = scroll * dashWidth
        var x = -dashWidth + offset
        while (x < size.width + dashWidth) {
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f),
                end = Offset(x + dashWidth * 0.55f, size.height / 2f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            x += dashWidth
        }
    }
}

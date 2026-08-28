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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TruckBody = Color(0xFF9BB8A0)
private val TruckBodyDark = Color(0xFF6E9580)
private val TruckWheel = Color(0xFF3F5C47)
private val TruckWindow = Color(0xFFE8F2EA)

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
    val wheelSpin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheels"
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
    val wheelAngle = if (animate) wheelSpin else progress * 25f

    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val scale = if (animate) 1f else 0.75f + progress * 0.25f
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f

        translate(centerX + offsetX, centerY + offsetY) {
            scale(scale, pivot = Offset.Zero) {
                drawTruck(wheelAngle)
            }
        }
    }
}

private fun DrawScope.drawTruck(wheelAngle: Float) {
    val w = size.width
    val h = size.height

    translate(-w / 2f, -h / 2f) {
        // Benne
        drawRoundRect(
            color = TruckBodyDark,
            topLeft = Offset(w * 0.34f, h * 0.12f),
            size = Size(w * 0.58f, h * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.08f)
        )
        // Bandes benne
        val stripeX = w * 0.42f
        val stripeW = w * 0.05f
        repeat(3) { index ->
            drawRoundRect(
                color = TruckBody,
                topLeft = Offset(stripeX + index * stripeW * 1.6f, h * 0.18f),
                size = Size(stripeW, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
            )
        }

        // Cabine
        drawRoundRect(
            color = TruckBody,
            topLeft = Offset(w * 0.04f, h * 0.28f),
            size = Size(w * 0.3f, h * 0.36f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.08f)
        )
        drawRoundRect(
            color = TruckWindow,
            topLeft = Offset(w * 0.08f, h * 0.34f),
            size = Size(w * 0.16f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.04f)
        )

        // Roues
        val frontWheel = Offset(w * 0.28f, h * 0.78f)
        val rearWheel = Offset(w * 0.72f, h * 0.78f)
        val wheelRadius = h * 0.14f

        drawWheel(frontWheel, wheelRadius, wheelAngle)
        drawWheel(rearWheel, wheelRadius, wheelAngle)
    }
}

private fun DrawScope.drawWheel(center: Offset, radius: Float, angle: Float) {
    drawCircle(color = TruckWheel, radius = radius, center = center)
    rotate(angle, center) {
        drawLine(
            color = TruckWindow,
            start = Offset(center.x - radius * 0.55f, center.y),
            end = Offset(center.x + radius * 0.55f, center.y),
            strokeWidth = radius * 0.22f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = TruckWindow,
            start = Offset(center.x, center.y - radius * 0.55f),
            end = Offset(center.x, center.y + radius * 0.55f),
            strokeWidth = radius * 0.22f,
            cap = StrokeCap.Round
        )
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

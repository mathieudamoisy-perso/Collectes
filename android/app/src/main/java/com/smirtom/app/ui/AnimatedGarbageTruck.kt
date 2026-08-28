package com.smirtom.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smirtom.app.R

@Composable
fun AnimatedGarbageTruck(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    animate: Boolean = true,
    pullProgress: Float = 0f,
) {
    if (animate) {
        DrivingGarbageTruck(modifier = modifier, size = size)
    } else {
        PullingGarbageTruck(modifier = modifier, size = size, pullProgress = pullProgress)
    }
}

@Composable
private fun DrivingGarbageTruck(
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val infinite = rememberInfiniteTransition(label = "garbageTruck")
    val driveOffset by infinite.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driveOffset"
    )
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(size * 1.6f)
                .height(size),
            contentAlignment = Alignment.Center
        ) {
            TruckRoad(
                modifier = Modifier
                    .width(size * 1.5f)
                    .height(6.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            Image(
                painter = painterResource(R.drawable.ic_launcher_fg),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        translationX = driveOffset
                        translationY = bounce
                    }
            )
        }
    }
}

@Composable
private fun PullingGarbageTruck(
    modifier: Modifier = Modifier,
    size: Dp,
    pullProgress: Float,
) {
    val truckScale = 0.82f + pullProgress.coerceIn(0f, 1f) * 0.18f

    Image(
        painter = painterResource(R.drawable.ic_launcher_fg),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = truckScale
                scaleY = truckScale
            }
    )
}

@Composable
private fun TruckRoad(
    modifier: Modifier = Modifier,
    color: Color,
) {
    val infinite = rememberInfiniteTransition(label = "truckRoad")
    val scroll by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roadScroll"
    )

    Canvas(modifier = modifier) {
        val dashWidth = size.width / 5f
        val offset = scroll * dashWidth
        var x = -dashWidth + offset
        while (x < size.width + dashWidth) {
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f),
                end = Offset((x + dashWidth * 0.55f).coerceAtMost(size.width), size.height / 2f),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            x += dashWidth
        }
    }
}

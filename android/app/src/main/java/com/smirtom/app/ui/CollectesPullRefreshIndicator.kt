package com.smirtom.app.ui

import android.graphics.PathMeasure as AndroidPathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.graphics.Path as AndroidPath
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val BrandGreen = Color(0xFF5E8F66)
private val BrandGreenDark = Color(0xFFA8C5AC)
private val RingSize = 44.dp
private val CheckmarkSize = 22.dp
private val RingStrokeWidth = 2.5.dp
private val LoadingTruckRoadGap = 3.dp
private val LoadingRoadHorizontalInset = 7.dp
private val LoadingSuspensionDurationMillis = 640
private val LoadingEnterDurationMillis = 360
private val LoadingTruckSuspensionAmplitude = 0.45.dp

private enum class RefreshIndicatorPhase {
    Hidden,
    Pulling,
    Loading,
    Success,
    Dismissing,
}

@Composable
private fun CollectesRefreshCheckmark(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas

        val checkPath = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.55f)
            lineTo(size.width * 0.42f, size.height * 0.72f)
            lineTo(size.width * 0.78f, size.height * 0.32f)
        }
        val measure = AndroidPathMeasure(checkPath.asAndroidPath(), false)
        val length = measure.length
        if (length <= 0f) return@Canvas

        val segment = AndroidPath()
        measure.getSegment(0f, length * progress.coerceIn(0f, 1f), segment, true)
        drawPath(
            path = segment.asComposePath(),
            color = color,
            style = Stroke(
                width = 2.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun CollectesRefreshDriveScene(
    drivePhase: Float,
    color: Color,
    roadCenterYFraction: Float,
    enterProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer { alpha = enterProgress },
    ) {
        if (enterProgress <= 0.01f) return@Canvas

        val roadY = size.height * roadCenterYFraction.coerceIn(0f, 1f)
        val dashLengthPx = 2.5.dp.toPx()
        val gapLengthPx = 3.5.dp.toPx()
        val dashStrokePx = 1.25.dp.toPx()
        val dashPeriodPx = dashLengthPx + gapLengthPx
        val scrollOffsetPx = (drivePhase % 1f) * dashPeriodPx

        var dashStartX = -dashPeriodPx + scrollOffsetPx
        while (dashStartX < size.width + dashPeriodPx) {
            val dashEndX = dashStartX + dashLengthPx
            val centerX = (dashStartX + dashEndX) / 2f
            if (centerX in 0f..size.width) {
                drawLine(
                    color = color.copy(alpha = 0.55f),
                    start = Offset(dashStartX, roadY),
                    end = Offset(dashEndX, roadY),
                    strokeWidth = dashStrokePx,
                    cap = StrokeCap.Round,
                )
            }
            dashStartX += dashPeriodPx
        }
    }
}

private fun loadingRoadCenterYFraction(
    circlePx: Float,
    truckHeightPx: Float,
    roadGapPx: Float,
    roadStrokePx: Float,
): Float {
    val centerPx = circlePx / 2f
    val truckBottomPx = centerPx + truckHeightPx / 2f
    val roadCenterYPx = truckBottomPx + roadGapPx + roadStrokePx / 2f
    return (roadCenterYPx / circlePx).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectesPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawPullProgress = state.distanceFraction.coerceIn(0f, 1f)
    var phase by remember { mutableStateOf(RefreshIndicatorPhase.Hidden) }
    var pullRingProgress by remember { mutableFloatStateOf(0f) }
    var pullGestureActive by remember { mutableStateOf(false) }
    val lockedRingProgress = remember { Animatable(0f) }
    val checkmarkAnim = remember { Animatable(0f) }
    val dismissAnim = remember { Animatable(1f) }
    val loadingEnterAnim = remember { Animatable(1f) }
    val latestOnComplete = rememberUpdatedState(onComplete)

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            checkmarkAnim.snapTo(0f)
            dismissAnim.snapTo(1f)
            lockedRingProgress.snapTo(pullRingProgress.coerceIn(0.98f, 1f))
            loadingEnterAnim.snapTo(0f)
            phase = RefreshIndicatorPhase.Loading
            loadingEnterAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LoadingEnterDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else if (phase == RefreshIndicatorPhase.Loading) {
            phase = RefreshIndicatorPhase.Success
            checkmarkAnim.snapTo(0f)
            checkmarkAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )
            delay(320)
            phase = RefreshIndicatorPhase.Dismissing
            dismissAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutSlowInEasing,
                ),
            )
            phase = RefreshIndicatorPhase.Hidden
            pullRingProgress = 0f
            pullGestureActive = false
            lockedRingProgress.snapTo(0f)
            checkmarkAnim.snapTo(0f)
            dismissAnim.snapTo(1f)
            loadingEnterAnim.snapTo(1f)
            latestOnComplete.value()
        }
    }

    LaunchedEffect(rawPullProgress, isRefreshing, phase) {
        if (isRefreshing || phase == RefreshIndicatorPhase.Loading ||
            phase == RefreshIndicatorPhase.Success ||
            phase == RefreshIndicatorPhase.Dismissing
        ) {
            return@LaunchedEffect
        }

        if (rawPullProgress <= 0.01f) {
            pullGestureActive = false
            pullRingProgress = 0f
            if (phase == RefreshIndicatorPhase.Pulling) {
                phase = RefreshIndicatorPhase.Hidden
            }
            return@LaunchedEffect
        }

        if (!pullGestureActive) {
            pullGestureActive = true
            if (rawPullProgress > 0.35f) {
                pullRingProgress = 0f
            } else {
                pullRingProgress = rawPullProgress
            }
            phase = RefreshIndicatorPhase.Pulling
        } else {
            pullRingProgress = rawPullProgress
            if (phase == RefreshIndicatorPhase.Hidden) {
                phase = RefreshIndicatorPhase.Pulling
            }
        }
    }

    val suspensionTransition = rememberInfiniteTransition(label = "suspension")
    val drivePhase by suspensionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LoadingSuspensionDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "suspensionPhase",
    )

    val density = LocalDensity.current
    val circlePx = with(density) { RingSize.toPx() }
    val isLoadingRoll = phase == RefreshIndicatorPhase.Loading
    val loadingTruckHeightPx = with(density) { CollectesTruckIconHeight.toPx() }
    val roadStrokePx = with(density) { 1.25.dp.toPx() }
    val roadCenterYFraction = if (isLoadingRoll) {
        loadingRoadCenterYFraction(
            circlePx = circlePx,
            truckHeightPx = loadingTruckHeightPx,
            roadGapPx = with(density) { LoadingTruckRoadGap.toPx() },
            roadStrokePx = roadStrokePx,
        )
    } else {
        0.78f
    }

    val loadingEnter = if (isLoadingRoll) loadingEnterAnim.value else 0f
    val truckSuspensionPx = if (isLoadingRoll) {
        sin(drivePhase * 2f * PI.toFloat()) *
            with(density) { LoadingTruckSuspensionAmplitude.toPx() } *
            loadingEnter
    } else {
        0f
    }

    val showIndicator = phase != RefreshIndicatorPhase.Hidden
    if (!showIndicator) return

    val ringProgress = when (phase) {
        RefreshIndicatorPhase.Pulling -> pullRingProgress
        RefreshIndicatorPhase.Loading, RefreshIndicatorPhase.Success, RefreshIndicatorPhase.Dismissing ->
            lockedRingProgress.value.coerceIn(0f, 1f)
        else -> 0f
    }

    val showTruck = phase == RefreshIndicatorPhase.Pulling ||
        phase == RefreshIndicatorPhase.Loading
    val showRoad = phase == RefreshIndicatorPhase.Loading
    val showCheckmark = phase == RefreshIndicatorPhase.Success ||
        phase == RefreshIndicatorPhase.Dismissing

    val hostAlpha = when (phase) {
        RefreshIndicatorPhase.Pulling -> pullRingProgress.coerceIn(0.45f, 1f)
        RefreshIndicatorPhase.Loading, RefreshIndicatorPhase.Success -> 1f
        RefreshIndicatorPhase.Dismissing -> dismissAnim.value
        else -> 1f
    }

    val tint = if (isSystemInDarkTheme()) BrandGreenDark else BrandGreen

    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .size(RingSize)
            .graphicsLayer { alpha = hostAlpha }
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = RingStrokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val trackAlpha = if (showCheckmark) 0.28f else 0.22f
            drawArc(
                color = tint.copy(alpha = trackAlpha),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            if (!showCheckmark) {
                val sweep = ringProgress.coerceIn(0f, 1f) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = tint,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            } else if (phase == RefreshIndicatorPhase.Success || phase == RefreshIndicatorPhase.Dismissing) {
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        if (showRoad) {
            CollectesRefreshDriveScene(
                drivePhase = drivePhase,
                color = tint,
                roadCenterYFraction = roadCenterYFraction,
                enterProgress = loadingEnter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LoadingRoadHorizontalInset),
            )
        }

        if (showTruck) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CollectesTruckBitmapIcon(
                    modifier = Modifier
                        .graphicsLayer { translationY = truckSuspensionPx }
                        .width(CollectesTruckIconWidth)
                        .height(CollectesTruckIconHeight),
                )
            }
        }

        if (showCheckmark) {
            CollectesRefreshCheckmark(
                progress = checkmarkAnim.value,
                color = tint,
                modifier = Modifier.size(CheckmarkSize),
            )
        }
    }
}

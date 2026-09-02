package com.smirtom.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Estimation du bandeau d'onglets avant mesure (hors barre système). */
val BottomBarTabRowHeight = 64.dp

val LocalBottomBarInset = compositionLocalOf { 0.dp }

val LocalBottomBarHideScroll = compositionLocalOf<NestedScrollConnection?> { null }

@Stable
class BottomBarScrollState(
    private val hideThresholdPx: Float
) {
    var isVisible by mutableStateOf(true)
        private set

    private var accumulatedScroll = 0f

    fun show() {
        isVisible = true
        accumulatedScroll = 0f
    }

    fun dispatchScroll(deltaY: Float) {
        if (deltaY < 0f) {
            accumulatedScroll += deltaY
            if (isVisible && accumulatedScroll <= -hideThresholdPx) {
                isVisible = false
            }
        } else if (deltaY > 0f) {
            accumulatedScroll = 0f
            if (!isVisible) {
                isVisible = true
            }
        }
    }
}

@Composable
fun rememberBottomBarScrollState(): BottomBarScrollState {
    val density = LocalDensity.current
    val hideThresholdPx = with(density) { 56.dp.toPx() }
    return remember { BottomBarScrollState(hideThresholdPx) }
}

@Composable
fun rememberBottomBarVisibility(bottomBarScrollState: BottomBarScrollState): Float {
    val target = if (bottomBarScrollState.isVisible) 1f else 0f
    return animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 220),
        label = "bottomBarVisibility"
    ).value
}

@Composable
fun rememberBottomBarInset(barHeight: Dp): Dp = barHeight

@Composable
fun rememberBottomBarFallbackHeight(): Dp {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return BottomBarTabRowHeight + navBarBottom + 12.dp
}

@Composable
fun rememberBottomBarHideScrollConnection(
    bottomBarScrollState: BottomBarScrollState
): NestedScrollConnection {
    return remember(bottomBarScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    bottomBarScrollState.dispatchScroll(available.y)
                }
                return Offset.Zero
            }
        }
    }
}

@Composable
fun rememberCollectesNestedScrollConnection(): NestedScrollConnection? {
    val pager = LocalPagerNestedScroll.current
    val hideBar = LocalBottomBarHideScroll.current
    if (pager == null) return null
    if (hideBar == null) return pager
    return remember(pager, hideBar) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                hideBar.onPreScroll(available, source)
                return pager.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = pager.onPostScroll(consumed, available, source)

            override suspend fun onPreFling(
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity = pager.onPreFling(available)

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity = pager.onPostFling(consumed, available)
        }
    }
}

@Composable
fun Modifier.collectesNestedScroll(): Modifier {
    val connection = rememberCollectesNestedScrollConnection() ?: return this
    return nestedScroll(connection)
}

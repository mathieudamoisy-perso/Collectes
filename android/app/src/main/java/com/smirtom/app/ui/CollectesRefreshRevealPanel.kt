package com.smirtom.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CollectesRefreshRevealMaxHeight: Dp = 132.dp

@OptIn(ExperimentalMaterial3Api::class)
fun collectesRefreshRevealHeight(
    state: PullToRefreshState,
    isRefreshing: Boolean
): Dp {
    val pullProgress = state.distanceFraction.coerceAtLeast(0f)
    return if (isRefreshing) {
        CollectesRefreshRevealMaxHeight
    } else {
        CollectesRefreshRevealMaxHeight * pullProgress.coerceAtMost(1.15f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectesRefreshRevealPanel(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pullProgress = state.distanceFraction.coerceAtLeast(0f)
    val revealHeight = collectesRefreshRevealHeight(state, isRefreshing)
    if (revealHeight <= 0.dp) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(revealHeight)
            .clipToBounds()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GarbageTruckCanvas(
            width = 88.dp,
            height = 48.dp,
            animate = isRefreshing,
            pullProgress = pullProgress.coerceIn(0f, 1f)
        )
        if (isRefreshing) {
            Spacer(modifier = Modifier.height(6.dp))
            TruckRoadCanvas(
                modifier = Modifier
                    .width(110.dp)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isRefreshing -> "Mise à jour des collectes…"
                pullProgress >= 1f -> "Relâchez pour actualiser"
                else -> "Tirez pour actualiser"
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

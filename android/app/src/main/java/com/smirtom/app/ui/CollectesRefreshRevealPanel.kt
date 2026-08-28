package com.smirtom.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val RevealMaxHeight = 112.dp
private const val ContentRevealThreshold = 0.25f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectesRefreshRevealPanel(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pullProgress = state.distanceFraction.coerceIn(0f, 1f)
    val targetHeight = when {
        isRefreshing -> RevealMaxHeight
        pullProgress > 0f -> RevealMaxHeight * pullProgress
        else -> 0.dp
    }
    val revealHeight by animateDpAsState(targetValue = targetHeight, label = "revealHeight")
    val showContent = isRefreshing || pullProgress >= ContentRevealThreshold

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(revealHeight)
            .clipToBounds()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (showContent) {
            val contentAlpha = when {
                isRefreshing -> 1f
                pullProgress < ContentRevealThreshold -> 0f
                else -> ((pullProgress - ContentRevealThreshold) / (1f - ContentRevealThreshold))
                    .coerceIn(0f, 1f)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .graphicsLayer { alpha = contentAlpha },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GarbageTruckCanvas(
                    width = if (isRefreshing) 88.dp else 72.dp,
                    height = if (isRefreshing) 48.dp else 40.dp,
                    animate = isRefreshing,
                    pullProgress = pullProgress
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (isRefreshing) {
                    TruckRoadCanvas(
                        modifier = Modifier
                            .width(104.dp)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mise à jour des collectes…",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = if (pullProgress >= 1f) {
                            "Relâchez pour actualiser"
                        } else {
                            "Tirez pour actualiser"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

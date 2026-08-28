package com.smirtom.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectesRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pullProgress = state.distanceFraction.coerceIn(0f, 1f)
    if (!isRefreshing && pullProgress <= 0.01f) return

    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0.88f + pullProgress * 0.12f,
        label = "refreshScale"
    )
    val alpha = if (isRefreshing) 1f else pullProgress.coerceIn(0.35f, 1f)

    Surface(
        modifier = modifier
            .pullToRefreshIndicator(state = state, isRefreshing = isRefreshing)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRefreshing) {
                AnimatedGarbageTruck(animate = true, size = 46.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mise à jour du calendrier…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AnimatedGarbageTruck(
                    animate = false,
                    pullProgress = pullProgress,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
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

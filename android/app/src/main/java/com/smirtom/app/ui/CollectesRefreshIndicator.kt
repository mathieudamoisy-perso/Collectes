package com.smirtom.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.zIndex

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
        targetValue = if (isRefreshing) 1f else 0.9f + pullProgress * 0.1f,
        label = "refreshScale"
    )
    val alpha = if (isRefreshing) 1f else pullProgress.coerceIn(0.5f, 1f)

    Surface(
        modifier = modifier
            .zIndex(2f)
            .pullToRefreshIndicator(state = state, isRefreshing = isRefreshing)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isRefreshing) {
                GarbageTruckCanvas(
                    width = 80.dp,
                    height = 44.dp,
                    animate = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                TruckRoadCanvas(
                    modifier = Modifier
                        .width(96.dp)
                        .height(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mise à jour du calendrier…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                GarbageTruckCanvas(
                    width = 72.dp,
                    height = 40.dp,
                    animate = false,
                    pullProgress = pullProgress
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (pullProgress >= 1f) {
                        "Relâchez pour actualiser"
                    } else {
                        "Tirez pour actualiser"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RefreshLoadingBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GarbageTruckCanvas(
                width = 96.dp,
                height = 52.dp,
                animate = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TruckRoadCanvas(
                modifier = Modifier
                    .width(120.dp)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Mise à jour du calendrier…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

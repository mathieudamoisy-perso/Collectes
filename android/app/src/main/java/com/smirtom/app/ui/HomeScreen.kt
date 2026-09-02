package com.smirtom.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.CollectionDay
import com.smirtom.app.data.SyncState
import com.smirtom.app.data.WasteType
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onFilterChange: (WasteType?) -> Unit
) {
    val isRefreshing = uiState.syncState is SyncState.Loading
    val showSkeleton = uiState.isLoadingNewCommune && isRefreshing
    val hasStaleContent = uiState.upcoming.isNotEmpty() || uiState.tomorrowWasteTypes.isNotEmpty()
    val contentAlpha by animateFloatAsState(
        targetValue = if (isRefreshing && hasStaleContent && !showSkeleton) 0.55f else 1f,
        animationSpec = tween(200),
        label = "refreshAlpha"
    )
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collectes de ${uiState.commune}") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            indicator = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CollectesRefreshRevealPanel(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .alpha(contentAlpha),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.syncState is SyncState.Error) {
                        item {
                            val sync = uiState.syncState as SyncState.Error
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Erreur de synchronisation", fontWeight = FontWeight.Bold)
                                    Text(sync.message)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = onRefresh) { Text("Réessayer") }
                                }
                            }
                        }
                    }

                    if (showSkeleton) {
                        item { TomorrowCardSkeleton() }
                        item {
                            WasteTypeFilterRow(
                                activeFilter = uiState.activeFilter,
                                onFilterChange = onFilterChange
                            )
                        }
                        item {
                            Text(
                                "Toutes les collectes",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(4) {
                            UpcomingItemSkeleton()
                        }
                    } else {
                        item {
                            AnimatedContent(
                                targetState = uiState.tomorrowWasteTypes,
                                transitionSpec = {
                                    fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                                },
                                label = "tomorrowCard"
                            ) { wasteTypes ->
                                TomorrowCard(
                                    tomorrowLabel = uiState.tomorrowLabel,
                                    wasteTypes = wasteTypes,
                                    activeFilter = uiState.activeFilter,
                                    showEmptyState = !isRefreshing
                                )
                            }
                        }

                        item {
                            WasteTypeFilterRow(
                                activeFilter = uiState.activeFilter,
                                onFilterChange = onFilterChange
                            )
                        }

                        item {
                            val listTitle = if (uiState.activeFilter == null) {
                                "Toutes les collectes (${uiState.upcoming.size})"
                            } else {
                                "Collectes ${uiState.activeFilter.label.lowercase()} (${uiState.upcoming.size})"
                            }
                            Text(listTitle, style = MaterialTheme.typography.titleMedium)
                        }

                        if (uiState.upcoming.isEmpty()) {
                            if (!isRefreshing) {
                                item {
                                    Text(
                                        text = "Aucune collecte à venir pour ce filtre.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                uiState.upcoming,
                                key = { "${it.date}_${it.wasteTypes.joinToString()}" }
                            ) { day ->
                                UpcomingItem(
                                    day = day,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasteTypeFilterRow(
    activeFilter: WasteType?,
    onFilterChange: (WasteType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == null,
            onClick = { onFilterChange(null) },
            label = { Text("Tous") },
            leadingIcon = {
                AllCollectionsIcon(
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        WasteType.entries.forEach { type ->
            val selected = activeFilter == type
            val palette = WasteTypeColors.palette(type)
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(if (selected) null else type) },
                label = { Text(type.label) },
                leadingIcon = {
                    WasteTypeIcon(
                        type = type,
                        size = 18.dp,
                        tint = palette.accent
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.containerMuted,
                    selectedContainerColor = palette.container,
                    labelColor = palette.accent,
                    selectedLabelColor = palette.accent,
                    iconColor = palette.accent,
                    selectedLeadingIconColor = palette.accent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = palette.accent.copy(alpha = 0.35f),
                    selectedBorderColor = palette.accent
                )
            )
        }
    }
}

@Composable
private fun TomorrowCard(
    tomorrowLabel: String,
    wasteTypes: List<WasteType>,
    activeFilter: WasteType?,
    showEmptyState: Boolean = true
) {
    val cardColor = if (wasteTypes.isEmpty()) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        WasteTypeColors.cardBackgroundOrDefault(wasteTypes)
    }
    val accentColor = wasteTypes.firstOrNull()?.let { WasteTypeColors.palette(it).accent }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                val title = if (activeFilter == null) "Demain" else "Demain — ${activeFilter.label}"
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tomorrowLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (wasteTypes.isEmpty()) {
                    if (showEmptyState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NoCollectionIcon(size = 20.dp)
                            Text(
                                "Rien à sortir",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    wasteTypes.forEach { type ->
                        WasteTypeLine(type)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingItem(
    day: CollectionDay,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    val accentColor = day.wasteTypes.firstOrNull()?.let { WasteTypeColors.palette(it).accent }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WasteTypeColors.cardBackgroundOrDefault(day.wasteTypes)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = day.date.format(formatter).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                day.wasteTypes.forEach { type ->
                    WasteTypeLine(type)
                }
            }
        }
    }
}

@Composable
private fun WasteTypeLine(type: WasteType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WasteTypeIcon(type = type, size = 20.dp)
        Text(
            text = "${type.label} (${type.colorName})",
            color = WasteTypeColors.palette(type).accent,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SkeletonPulseBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

@Composable
private fun TomorrowCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonPulseBox(modifier = Modifier.fillMaxWidth(0.35f), height = 24.dp)
            SkeletonPulseBox(modifier = Modifier.fillMaxWidth(0.55f), height = 18.dp)
            SkeletonPulseBox(modifier = Modifier.fillMaxWidth(0.45f), height = 18.dp)
        }
    }
}

@Composable
private fun UpcomingItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonPulseBox(modifier = Modifier.fillMaxWidth(0.5f), height = 18.dp)
            SkeletonPulseBox(modifier = Modifier.fillMaxWidth(0.4f), height = 16.dp)
        }
    }
}

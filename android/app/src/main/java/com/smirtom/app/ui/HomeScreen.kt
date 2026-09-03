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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.CollectionDay
import com.smirtom.app.data.SyncState
import com.smirtom.app.data.VexinCommune
import com.smirtom.app.data.WasteStreamGuide
import com.smirtom.app.data.WasteStreamGuides
import com.smirtom.app.data.WasteType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    commune: VexinCommune,
    communes: List<VexinCommune>,
    onCommuneSelected: (VexinCommune) -> Unit,
    onRefresh: () -> Unit,
    onFilterChange: (WasteType?) -> Unit,
    onNextCollectionClick: (WasteType) -> Unit,
    nextCollectionDate: suspend (WasteType) -> LocalDate?,
    modifier: Modifier = Modifier
) {
    val guides = remember(commune.slug) { WasteStreamGuides.forCommune(commune) }
    var selectedGuide by remember { mutableStateOf<WasteStreamGuide?>(null) }
    var showCommunePicker by remember { mutableStateOf(false) }

    fun openGuideDetail(type: WasteType) {
        selectedGuide = guides.find { it.type == type }
    }
    val isRefreshing = uiState.syncState is SyncState.Loading
    val hasStaleContent = uiState.upcoming.isNotEmpty() || uiState.tomorrowWasteTypes.isNotEmpty()
    val showSkeleton = uiState.isLoadingNewCommune || uiState.isInitialLoading ||
        (isRefreshing && !hasStaleContent)
    val contentAlpha by animateFloatAsState(
        targetValue = if (isRefreshing && hasStaleContent && !showSkeleton) 0.55f else 1f,
        animationSpec = tween(200),
        label = "refreshAlpha"
    )
    val pullRefreshState = rememberPullToRefreshState()
    val pullRefreshScope = rememberCoroutineScope()
    val pullRefreshEnabled = !showSkeleton
    var refreshFromPull by remember { mutableStateOf(false) }

    val showPullRefresh = isRefreshing && refreshFromPull
    val bottomInset = LocalBottomBarInset.current * LocalBottomBarVisibility.current

    Column(modifier = modifier.fillMaxSize()) {
        CollectesAppHeader(
            communeName = uiState.commune,
            onCommuneClick = { showCommunePicker = true }
        )
        if (showCommunePicker) {
            CommunePickerBottomSheet(
                communes = communes,
                selectedSlug = commune.slug,
                onSelect = onCommuneSelected,
                onDismiss = { showCommunePicker = false }
            )
        }
        PullToRefreshBox(
            isRefreshing = showPullRefresh,
            onRefresh = {
                if (pullRefreshEnabled) {
                    refreshFromPull = true
                    onRefresh()
                }
            },
            state = pullRefreshState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            indicator = {
                CollectesPullRefreshIndicator(
                    state = pullRefreshState,
                    isRefreshing = showPullRefresh,
                    onComplete = {
                        refreshFromPull = false
                        pullRefreshScope.launch { pullRefreshState.snapTo(0f) }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha)
                    .collectesNestedScroll(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 21.dp + bottomInset
                ),
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
                    FilterRowSkeleton()
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
                        contentKey = { types -> types.joinToString { it.name } },
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                        },
                        label = "tomorrowCard"
                    ) { wasteTypes ->
                        TomorrowCard(
                            tomorrowLabel = uiState.tomorrowLabel,
                            wasteTypes = wasteTypes,
                            onTypeClick = ::openGuideDetail
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
                            modifier = Modifier
                        )
                    }
                }
            }
        }
        }
    }

    selectedGuide?.let { guide ->
        WasteGuideDetailBottomSheet(
            guide = guide,
            commune = commune,
            onDismiss = { selectedGuide = null },
            onNextCollectionClick = onNextCollectionClick,
            nextCollectionDate = nextCollectionDate,
            showNextCollection = false
        )
    }
}

@Composable
private fun FilterRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkeletonPulseBox(modifier = Modifier.width(72.dp), height = 32.dp)
        SkeletonPulseBox(modifier = Modifier.width(96.dp), height = 32.dp)
        SkeletonPulseBox(modifier = Modifier.width(88.dp), height = 32.dp)
        SkeletonPulseBox(modifier = Modifier.width(104.dp), height = 32.dp)
        SkeletonPulseBox(modifier = Modifier.width(92.dp), height = 32.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasteTypeFilterRow(
    activeFilter: WasteType?,
    onFilterChange: (WasteType?) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pagerNestedScroll()
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
            val palette = remember(type, darkTheme) { WasteTypeColors.paletteFor(type, darkTheme) }
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
    onTypeClick: (WasteType) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val cardColor = if (wasteTypes.isEmpty()) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        WasteTypeColors.cardBackgroundOrDefault(wasteTypes)
    }
    val accentColor = remember(wasteTypes, darkTheme) {
        wasteTypes.firstOrNull()?.let { WasteTypeColors.paletteFor(it, darkTheme).accent }
    }

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
                Text("Demain", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tomorrowLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (wasteTypes.isEmpty()) {
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
                } else {
                    wasteTypes.forEach { type ->
                        WasteTypeLine(
                            type = type,
                            onClick = { onTypeClick(type) },
                            showInfoIcon = true
                        )
                    }
                }
            }
        }
    }
}

private val upcomingDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

@Composable
private fun UpcomingItem(
    day: CollectionDay,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val accentColor = remember(day.wasteTypes, darkTheme) {
        day.wasteTypes.firstOrNull()?.let { WasteTypeColors.paletteFor(it, darkTheme).accent }
    }

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
                    text = day.date.format(upcomingDateFormatter).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                day.wasteTypes.forEach { type ->
                    WasteTypeLine(type = type)
                }
            }
        }
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
            .clip(RoundedCornerShape(16.dp))
            .alpha(alpha)
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

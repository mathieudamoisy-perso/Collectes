package com.smirtom.app.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smirtom.app.R
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Commune : ${uiState.commune}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

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

                item {
                    TomorrowCard(
                        tomorrowLabel = uiState.tomorrowLabel,
                        wasteTypes = uiState.tomorrowWasteTypes,
                        activeFilter = uiState.activeFilter
                    )
                }

                item {
                    WasteTypeFilterRow(
                        activeFilter = uiState.activeFilter,
                        onFilterChange = onFilterChange
                    )
                }

                item {
                    val listTitle = if (uiState.activeFilter == null) {
                        "Toutes les échéances (${uiState.upcoming.size})"
                    } else {
                        "Échéances ${uiState.activeFilter.label.lowercase()} (${uiState.upcoming.size})"
                    }
                    Text(listTitle, style = MaterialTheme.typography.titleMedium)
                }

                if (uiState.upcoming.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune collecte à venir pour ce filtre.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        uiState.upcoming,
                        key = { "${it.date}_${it.wasteTypes.joinToString()}" }
                    ) { day ->
                        UpcomingItem(day)
                    }
                }

                item {
                    Text(
                        text = "Tirez vers le bas pour actualiser le calendrier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
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
                    tint = if (activeFilter == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )
        WasteType.entries.forEach { type ->
            val selected = activeFilter == type
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(if (selected) null else type) },
                label = {
                    Text(
                        text = type.label,
                        color = if (selected) WasteTypeColors.accent(type) else MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    WasteTypeIcon(
                        type = type,
                        size = 18.dp,
                        tint = if (selected) {
                            WasteTypeColors.accent(type)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = WasteTypeColors.cardBackground(type),
                    selectedContainerColor = WasteTypeColors.cardBackground(type),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun TomorrowCard(
    tomorrowLabel: String,
    wasteTypes: List<WasteType>,
    activeFilter: WasteType?
) {
    val cardColor = if (wasteTypes.isEmpty()) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        WasteTypeColors.cardBackgroundOrDefault(wasteTypes)
    }
    val accentColor = wasteTypes.firstOrNull()?.let { WasteTypeColors.accent(it) }

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
                        WasteTypeLine(type)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingItem(day: CollectionDay) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    val accentColor = day.wasteTypes.firstOrNull()?.let { WasteTypeColors.accent(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            color = WasteTypeColors.accent(type),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

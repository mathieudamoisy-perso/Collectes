package com.smirtom.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smirtom") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Commune : ${uiState.commune}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            when (val sync = uiState.syncState) {
                is SyncState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SyncState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Erreur de synchronisation", fontWeight = FontWeight.Bold)
                            Text(sync.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text("Réessayer") }
                        }
                    }
                }
                else -> Unit
            }

            TomorrowCard(
                tomorrowLabel = uiState.tomorrowLabel,
                wasteTypes = uiState.tomorrowWasteTypes,
                activeFilter = uiState.activeFilter
            )

            WasteTypeFilterRow(
                activeFilter = uiState.activeFilter,
                onFilterChange = onFilterChange
            )

            val listTitle = if (uiState.activeFilter == null) {
                "Toutes les échéances (${uiState.upcoming.size})"
            } else {
                "Échéances ${uiState.activeFilter.label.lowercase()} (${uiState.upcoming.size})"
            }
            Text(listTitle, style = MaterialTheme.typography.titleMedium)

            if (uiState.upcoming.isEmpty()) {
                Text(
                    text = "Aucune collecte à venir pour ce filtre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.upcoming, key = { "${it.date}_${it.wasteTypes.joinToString()}" }) { day ->
                        UpcomingItem(day)
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
            label = { Text("Tous") }
        )
        WasteType.entries.forEach { type ->
            FilterChip(
                selected = activeFilter == type,
                onClick = { onFilterChange(if (activeFilter == type) null else type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = WasteTypeColors.cardBackground(type),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (activeFilter == null) "Demain" else "Demain — ${activeFilter.label}"
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(tomorrowLabel, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (wasteTypes.isEmpty()) {
                Text("Rien à sortir")
            } else {
                wasteTypes.forEach { type ->
                    Text("• ${type.label} (${type.colorName})")
                }
            }
        }
    }
}

@Composable
private fun UpcomingItem(day: CollectionDay) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WasteTypeColors.cardBackgroundOrDefault(day.wasteTypes)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = day.date.format(formatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
                },
                fontWeight = FontWeight.SemiBold
            )
            Text(day.wasteTypes.joinToString(" + ") { "${it.label} (${it.colorName})" })
        }
    }
}

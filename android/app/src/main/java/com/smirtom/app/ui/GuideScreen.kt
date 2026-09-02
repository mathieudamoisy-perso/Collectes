package com.smirtom.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.VexinCommune
import com.smirtom.app.data.WasteStreamGuide
import com.smirtom.app.data.WasteStreamGuides
import com.smirtom.app.data.WasteType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    commune: VexinCommune,
    initialDetailType: WasteType?,
    onInitialDetailConsumed: () -> Unit,
    onNextCollectionClick: (WasteType) -> Unit,
    nextCollectionDate: suspend (WasteType) -> LocalDate?,
    modifier: Modifier = Modifier
) {
    val guides = remember(commune.slug) { WasteStreamGuides.forCommune(commune) }
    var selectedGuide by remember { mutableStateOf<WasteStreamGuide?>(null) }
    val bottomInset = LocalBottomBarInset.current

    LaunchedEffect(initialDetailType, guides) {
        if (initialDetailType != null) {
            selectedGuide = guides.find { it.type == initialDetailType }
            onInitialDetailConsumed()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .pagerNestedScroll(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp + bottomInset
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            CompactScreenHeader(title = "Guide du tri", horizontalPadding = 0.dp)
        }
        item(span = { GridItemSpan(2) }) {
            GuideSourceBanner(commune = commune)
        }
        items(guides, key = { it.type.name }) { guide ->
            WasteBinCard(
                guide = guide,
                onClick = { selectedGuide = guide }
            )
        }
    }

    selectedGuide?.let { guide ->
        WasteGuideDetailBottomSheet(
            guide = guide,
            commune = commune,
            onDismiss = { selectedGuide = null },
            onNextCollectionClick = onNextCollectionClick,
            nextCollectionDate = nextCollectionDate,
            showNextCollectionBorder = true
        )
    }
}

@Composable
private fun GuideSourceBanner(
    commune: VexinCommune,
    modifier: Modifier = Modifier
) {
    val subtitle = commune.guideSourceSubtitle()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    text = commune.guideSourceTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

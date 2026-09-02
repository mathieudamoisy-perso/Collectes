package com.smirtom.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.VexinCommune
import com.smirtom.app.data.WasteStreamGuide
import com.smirtom.app.data.WasteType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteGuideDetailBottomSheet(
    guide: WasteStreamGuide,
    commune: VexinCommune,
    onDismiss: () -> Unit,
    onNextCollectionClick: (WasteType) -> Unit,
    nextCollectionDate: suspend (WasteType) -> LocalDate?,
    showNextCollection: Boolean = true,
    showNextCollectionBorder: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        WasteGuideDetailContent(
            guide = guide,
            commune = commune,
            onNextCollectionClick = {
                onDismiss()
                onNextCollectionClick(guide.type)
            },
            nextCollectionDate = nextCollectionDate,
            showNextCollection = showNextCollection,
            showNextCollectionBorder = showNextCollectionBorder
        )
    }
}

@Composable
fun WasteGuideDetailContent(
    guide: WasteStreamGuide,
    commune: VexinCommune,
    onNextCollectionClick: () -> Unit,
    nextCollectionDate: suspend (WasteType) -> LocalDate?,
    showNextCollection: Boolean = true,
    showNextCollectionBorder: Boolean = false
) {
    val palette = WasteTypeColors.palette(guide.type)
    val context = LocalContext.current
    var nextDate by remember(guide.type) { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(guide.type, showNextCollection) {
        nextDate = if (showNextCollection) nextCollectionDate(guide.type) else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(palette.container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                WasteTypeIcon(type = guide.type, size = 32.dp, tint = palette.accent)
            }
            Column {
                Text(
                    text = guide.type.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
                Text(
                    text = guide.type.guideSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (showNextCollection) {
            nextDate?.let { date ->
            val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
            val label = date.format(formatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNextCollectionClick),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = if (showNextCollectionBorder) {
                    BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.45f))
                } else {
                    null
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Prochaine collecte",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            }
        }

        WasteGuideSection(
            title = "À mettre",
            items = guide.acceptedItems,
            icon = Icons.Default.CheckCircle,
            iconTint = palette.accent
        )

        Spacer(modifier = Modifier.height(16.dp))

        WasteGuideSection(
            title = "À ne pas mettre",
            items = guide.rejectedItems,
            icon = Icons.Default.Cancel,
            iconTint = MaterialTheme.colorScheme.error
        )

        if (guide.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.containerMuted)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    guide.tips.forEach { tip ->
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(commune.guideInfoUrl()))
                )
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = commune.guideInfoLinkLabel())
        }

        commune.guideSecondaryInfoUrl()?.let { secondaryUrl ->
            commune.guideSecondaryInfoLinkLabel()?.let { secondaryLabel ->
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(secondaryUrl)))
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = secondaryLabel)
                }
            }
        }
    }
}

@Composable
private fun WasteGuideSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

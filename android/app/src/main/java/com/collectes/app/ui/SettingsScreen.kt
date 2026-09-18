package com.collectes.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.collectes.app.R
import com.collectes.app.data.ReminderTime
import com.collectes.app.data.SyncState
import com.collectes.app.data.SyncStatusFormatter
import com.collectes.app.util.BatteryOptimizationHelper
import com.collectes.app.util.FeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    showBack: Boolean = true,
    modifier: Modifier = Modifier
) {
    val reminderTimeMinutes by viewModel.reminderTimeMinutes.collectAsState()
    val selectedCommune by viewModel.selectedCommune.collectAsState()
    val calendarError by viewModel.calendarError.collectAsState()
    val useBrandColors by viewModel.useBrandColors.collectAsState()
    val enabledReminderTypes by viewModel.enabledReminderTypes.collectAsState()
    val availableReminderTypes by viewModel.availableReminderTypes.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var reminderTimeMenuExpanded by remember { mutableStateOf(false) }
    val reminderTimeOptions = remember { ReminderTime.options() }
    var ignoringBatteryOptimizations by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var feedbackEmailError by remember { mutableStateOf<String?>(null) }
    var feedbackWhatsAppError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBatteryOptimizations =
                    BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bottomInset = if (!showBack) LocalBottomBarInset.current else 0.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(if (!showBack) Modifier.pagerNestedScroll() else Modifier),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp + bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (showBack) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Text(
                        "Réglages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                CompactScreenHeader(title = "Réglages", horizontalPadding = 0.dp)
            }
        }

            item {
                SettingsSectionCard {
                    SettingsSectionHeader(
                        icon = Icons.Default.Notifications,
                        title = "Rappel"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choisissez l’heure de la notification la veille de la collecte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = reminderTimeMenuExpanded,
                        onExpandedChange = { reminderTimeMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = ReminderTime.format(reminderTimeMinutes),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Heure du rappel") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = reminderTimeMenuExpanded,
                            onDismissRequest = { reminderTimeMenuExpanded = false }
                        ) {
                            reminderTimeOptions.forEach { minutes ->
                                DropdownMenuItem(
                                    text = { Text(ReminderTime.format(minutes)) },
                                    onClick = {
                                        reminderTimeMenuExpanded = false
                                        if (minutes != reminderTimeMinutes) {
                                            viewModel.setReminderTime(minutes)
                                        }
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Types de bac à rappeler",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    availableReminderTypes.forEach { type ->
                        val palette = remember(type, darkTheme) {
                            WasteTypeColors.paletteFor(type, darkTheme)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WasteTypeIcon(
                                type = type,
                                size = 20.dp,
                                tint = palette.accent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = type in enabledReminderTypes,
                                onCheckedChange = { enabled ->
                                    viewModel.setReminderTypeEnabled(type, enabled)
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard {
                    SettingsSectionHeader(
                        icon = Icons.Default.Palette,
                        title = "Apparence"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vert Collectes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (useBrandColors) {
                                    "Couleurs de l’app"
                                } else {
                                    "Thème du téléphone"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useBrandColors,
                            onCheckedChange = { viewModel.setUseBrandColors(it) }
                        )
                    }
                }
            }

            if (!ignoringBatteryOptimizations) {
                item {
                    SettingsSectionCard {
                        SettingsSectionHeader(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "Pour des rappels à l’heure"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Android met parfois les apps en veille pour économiser la batterie. " +
                                "Résultat : le rappel de collecte peut arriver en retard, ou pas du tout.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Autoriser Collectes à fonctionner normalement aide uniquement les notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { BatteryOptimizationHelper.openAppBatterySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Autoriser les rappels à l’heure")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sur l’écran suivant, choisissez Autoriser ou Sans restriction. " +
                                "Vous pourrez revenir en arrière à tout moment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SettingsSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(viewModel.officialCalendarViewUrl())
                                ).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                }
                                runCatching { context.startActivity(intent) }
                                    .onFailure { viewModel.reportCalendarOpenError() }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calendrier officiel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedCommune.officialCalendarSubtitle(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Ouvrir dans le navigateur",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    calendarError?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    when (val sync = syncState) {
                        is SyncState.Success -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = SyncStatusFormatter.format(sync.lastSync),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> Unit
                    }
                }
            }

            item {
                SettingsSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(context.getString(R.string.privacy_policy_url))
                                ).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Politique de confidentialité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Données locales, aucun compte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Ouvrir dans le navigateur",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                SettingsSectionCard {
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Email,
                        title = "Me contacter"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Un bug, une idée ou un mot sympa ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsContactOptionRow(
                        icon = Icons.Outlined.Email,
                        title = "Par mail",
                        subtitle = "Via votre application mail",
                        onClick = {
                            feedbackEmailError = null
                            FeedbackHelper.openDeveloperEmail(
                                context = context,
                                recipient = context.getString(R.string.developer_contact_email),
                                subject = context.getString(R.string.feedback_email_subject),
                                appVersion = versionName,
                                communeName = selectedCommune.displayName
                            ).onFailure {
                                feedbackEmailError = "Impossible d'ouvrir l'application mail"
                            }
                        },
                        openContentDescription = "Ouvrir l'application mail"
                    )
                    feedbackEmailError?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsContactOptionRow(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        title = "Par WhatsApp",
                        subtitle = "Message direct",
                        onClick = {
                            feedbackWhatsAppError = null
                            FeedbackHelper.openDeveloperWhatsApp(
                                context = context,
                                phoneE164 = context.getString(R.string.developer_whatsapp_phone),
                                appVersion = versionName,
                                communeName = selectedCommune.displayName
                            ).onFailure {
                                feedbackWhatsAppError = "Impossible d'ouvrir WhatsApp"
                            }
                        },
                        openContentDescription = "Ouvrir WhatsApp"
                    )
                    feedbackWhatsAppError?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version $versionName",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
}

@Composable
private fun SettingsContactOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    openContentDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = openContentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

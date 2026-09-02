package com.smirtom.app.ui

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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.material3.Text
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
import com.smirtom.app.R
import com.smirtom.app.data.ReminderTime
import com.smirtom.app.util.BatteryOptimizationHelper
import com.smirtom.app.util.FeedbackHelper

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
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var communeMenuExpanded by remember { mutableStateOf(false) }
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
                        icon = Icons.Default.Place,
                        title = "Commune"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = communeMenuExpanded,
                        onExpandedChange = { communeMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCommune.displayName,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = communeMenuExpanded,
                            onDismissRequest = { communeMenuExpanded = false }
                        ) {
                            viewModel.communes.forEach { commune ->
                                DropdownMenuItem(
                                    text = { Text(commune.displayName) },
                                    onClick = {
                                        communeMenuExpanded = false
                                        if (commune.slug != selectedCommune.slug) {
                                            viewModel.setCommune(commune)
                                        }
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
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
                        text = "Notification la veille de la collecte",
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
                }
            }

            if (!ignoringBatteryOptimizations) {
                item {
                    SettingsSectionCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.BatteryAlert,
                            title = "Optimisation batterie",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Désactivez l'optimisation pour recevoir les rappels à l'heure prévue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { BatteryOptimizationHelper.openAppBatterySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ouvrir les paramètres batterie")
                        }
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

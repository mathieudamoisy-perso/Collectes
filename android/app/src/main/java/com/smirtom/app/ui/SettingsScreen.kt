package com.smirtom.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.util.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val reminderHour by viewModel.reminderHour.collectAsState()
    val selectedCommune by viewModel.selectedCommune.collectAsState()
    val calendarError by viewModel.calendarError.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var communeMenuExpanded by remember { mutableStateOf(false) }
    var ignoringBatteryOptimizations by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Commune", style = MaterialTheme.typography.titleMedium)
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

            Column {
                Text(
                    text = "Rappel la veille à ${reminderHour}h00",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Notification le jour précédant la collecte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = reminderHour.toFloat(),
                    onValueChange = { viewModel.setReminderHour(it.toInt()) },
                    valueRange = PreferencesManager.MIN_REMINDER_HOUR.toFloat()
                        ..PreferencesManager.MAX_REMINDER_HOUR.toFloat(),
                    steps = PreferencesManager.MAX_REMINDER_HOUR - PreferencesManager.MIN_REMINDER_HOUR - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${PreferencesManager.MIN_REMINDER_HOUR}h — ${PreferencesManager.MAX_REMINDER_HOUR}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!ignoringBatteryOptimizations) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { BatteryOptimizationHelper.openAppBatterySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Désactiver l'optimisation de la batterie")
                    }
                    Text(
                        text = "Ouvre les paramètres de l'application pour désactiver l'optimisation de la batterie et garantir la bonne réception des notifications push la veille des jours de collecte",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(viewModel.officialCalendarViewUrl())
                    ).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure { viewModel.reportCalendarOpenError() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir le calendrier officiel")
            }
            Text(
                text = "Affiche le PDF SMIRTOM de ${selectedCommune.displayName} dans le navigateur, sans l'enregistrer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            calendarError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

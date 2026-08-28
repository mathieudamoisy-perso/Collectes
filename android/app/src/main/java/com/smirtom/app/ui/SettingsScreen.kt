package com.smirtom.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.data.SmirtomFetcher
import com.smirtom.app.util.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val reminderHour by viewModel.reminderHour.collectAsState()
    val selectedCommune by viewModel.selectedCommune.collectAsState()
    val context = LocalContext.current
    var communeMenuExpanded by remember { mutableStateOf(false) }

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
                Text(
                    text = "Communes du SMIRTOM du Vexin uniquement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

            OutlinedButton(
                onClick = { BatteryOptimizationHelper.openAppBatterySettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Optimisation batterie")
            }

            Text(
                text = "Ouvre les paramètres Android de l'application pour désactiver l'optimisation batterie et garantir les rappels.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SmirtomFetcher.DOWNLOADS_URL))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir le site SMIRTOM")
            }
        }
    }
}

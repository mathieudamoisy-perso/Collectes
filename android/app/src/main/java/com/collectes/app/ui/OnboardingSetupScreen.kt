package com.collectes.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.collectes.app.data.ReminderTime
import com.collectes.app.data.VexinCommune

private enum class OnboardingStep { Commune, ReminderTime, Reliability }

@Composable
fun OnboardingSetupScreen(
    onCommuneSelected: (VexinCommune) -> Unit = {},
    onSetupComplete: (commune: VexinCommune, reminderTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Commune) }
    var selectedCommune by remember { mutableStateOf<VexinCommune?>(null) }
    var selectedReminderMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    when (step) {
        OnboardingStep.Commune -> CommuneSetupStep(
            onCommuneChosen = { commune ->
                selectedCommune = commune
                onCommuneSelected(commune)
                step = OnboardingStep.ReminderTime
            },
            modifier = modifier
        )
        OnboardingStep.ReminderTime -> {
            if (selectedCommune == null) {
                step = OnboardingStep.Commune
            } else {
                ReminderTimeSetupStep(
                    onBack = { step = OnboardingStep.Commune },
                    onConfirm = { minutes ->
                        selectedReminderMinutes = minutes
                        step = OnboardingStep.Reliability
                    },
                    modifier = modifier
                )
            }
        }
        OnboardingStep.Reliability -> {
            val commune = selectedCommune
            val minutes = selectedReminderMinutes
            if (commune == null || minutes == null) {
                step = OnboardingStep.Commune
            } else {
                ReliabilitySetupScreen(
                    onBack = { step = OnboardingStep.ReminderTime },
                    onContinue = { onSetupComplete(commune, minutes) },
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun CommuneSetupStep(
    onCommuneChosen: (VexinCommune) -> Unit,
    modifier: Modifier = Modifier
) {
    CommuneSearchPanel(
        onCommuneChosen = onCommuneChosen,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        intro = {
            Text(
                text = "Bienvenue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Étape 1 sur 3 · Commune",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recherchez votre commune ou utilisez votre position. " +
                    "L’application trouve le calendrier automatiquement si la commune est couverte.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    )
}

@Composable
private fun ReminderTimeSetupStep(
    onBack: () -> Unit,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { ReminderTime.options() }
    var selectedMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour"
                )
            }
            Text(
                text = "Heure du rappel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Étape 2 sur 3 · Notification",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "À quelle heure souhaitez-vous être prévenu la veille d’une collecte ?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(options, key = { it }) { minutes ->
                val selected = minutes == selectedMinutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMinutes = minutes }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = ReminderTime.format(minutes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        Button(
            onClick = { selectedMinutes?.let(onConfirm) },
            enabled = selectedMinutes != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = selectedMinutes?.let {
                    "Recevoir le rappel à ${ReminderTime.format(it)}"
                } ?: "Choisissez une heure"
            )
        }
    }
}

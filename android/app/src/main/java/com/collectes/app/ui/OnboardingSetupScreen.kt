package com.collectes.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.collectes.app.data.NearestCommuneFinder
import com.collectes.app.data.NearestCommuneMatch
import com.collectes.app.data.ReminderTime
import com.collectes.app.data.VexinCommune
import com.collectes.app.util.DeviceLocationHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class OnboardingStep { Commune, ReminderTime }

@Composable
fun OnboardingSetupScreen(
    communes: List<VexinCommune>,
    onSetupComplete: (commune: VexinCommune, reminderTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Commune) }
    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCommune = remember(selectedSlug, communes) {
        selectedSlug?.let { slug -> communes.find { it.slug == slug } }
    }

    when (step) {
        OnboardingStep.Commune -> CommuneSetupStep(
            communes = communes,
            onCommuneChosen = { commune ->
                selectedSlug = commune.slug
                step = OnboardingStep.ReminderTime
            },
            modifier = modifier
        )
        OnboardingStep.ReminderTime -> {
            val commune = selectedCommune
            if (commune == null) {
                step = OnboardingStep.Commune
            } else {
                ReminderTimeSetupStep(
                    onBack = { step = OnboardingStep.Commune },
                    onConfirm = { minutes -> onSetupComplete(commune, minutes) },
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun CommuneSetupStep(
    communes: List<VexinCommune>,
    onCommuneChosen: (VexinCommune) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationHelper = remember { DeviceLocationHelper(context) }
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestionSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestionDistanceKm by rememberSaveable { mutableStateOf<Double?>(null) }
    val suggestion = remember(suggestionSlug, suggestionDistanceKm, communes) {
        val slug = suggestionSlug ?: return@remember null
        val distanceKm = suggestionDistanceKm ?: return@remember null
        val commune = communes.find { it.slug == slug } ?: return@remember null
        NearestCommuneMatch(commune, distanceKm)
    }

    fun applyNearestFromDevice() {
        scope.launch {
            locating = true
            locationError = null
            suggestionSlug = null
            suggestionDistanceKm = null
            val location = locationHelper.readBestLastKnownLocation()
            locating = false
            if (location == null) {
                locationError =
                    "Position introuvable. Activez la localisation ou choisissez une commune dans la liste."
                return@launch
            }
            val match = NearestCommuneFinder.findNearest(
                location.latitude,
                location.longitude,
                communes
            )
            if (match == null) {
                locationError = "Aucune commune disponible."
                return@launch
            }
            suggestionSlug = match.commune.slug
            suggestionDistanceKm = match.distanceKm
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            applyNearestFromDevice()
        } else {
            locationError =
                "Permission refusée. Vous pouvez choisir votre commune manuellement."
        }
    }

    fun requestLocation() {
        locationError = null
        if (locationHelper.hasLocationPermission()) {
            applyNearestFromDevice()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item(key = "intro") {
            Text(
                text = "Bienvenue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Étape 1 sur 2 · Commune",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choisissez votre commune pour afficher les collectes. " +
                    "Vous pourrez la changer plus tard.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { requestLocation() },
                enabled = !locating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Recherche de la commune…")
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Utiliser ma position")
                }
            }

            locationError?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        suggestion?.let { match ->
            item(key = "suggestion") {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Commune la plus proche",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = match.commune.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatDistanceLabel(match.distanceKm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (match.distanceKm > NearestCommuneFinder.FAR_AWAY_THRESHOLD_KM) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Vous semblez loin des communes couvertes — vérifiez le choix.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onCommuneChosen(match.commune) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continuer avec ${match.commune.displayName}")
                        }
                    }
                }
            }
        }

        item(key = "list_header") {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Ou choisissez dans la liste",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(communes, key = { it.slug }) { commune ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCommuneChosen(commune) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = commune.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }

        item(key = "privacy") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "La position reste sur l’appareil et n’est pas envoyée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
            text = "Étape 2 sur 2 · Notification",
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

private fun formatDistanceLabel(distanceKm: Double): String {
    return if (distanceKm < 1.0) {
        "À environ ${(distanceKm * 1000).roundToInt()} m"
    } else {
        "À environ ${distanceKm.roundToInt()} km"
    }
}

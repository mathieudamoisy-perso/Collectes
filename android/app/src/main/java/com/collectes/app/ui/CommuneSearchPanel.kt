package com.collectes.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.collectes.app.data.CommuneCoverageResolver
import com.collectes.app.data.CommuneCoverageResult
import com.collectes.app.data.CommunesIndexRepository
import com.collectes.app.data.GeoApiGouvClient
import com.collectes.app.data.GeoCommune
import com.collectes.app.data.VexinCommune
import com.collectes.app.util.DeviceLocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Recherche France (geo.api.gouv) + localisation + résolution couverture.
 * Mutualisé onboarding / changement de commune.
 */
@Composable
fun CommuneSearchPanel(
    onCommuneChosen: (VexinCommune) -> Unit,
    intro: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val context = LocalContext.current
    val locationHelper = remember { DeviceLocationHelper(context) }
    val indexRepository = remember { CommunesIndexRepository.get(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { indexRepository.ensureLoaded() }
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<GeoCommune>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by rememberSaveable { mutableStateOf<String?>(null) }

    var locating by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }
    var coverageError by rememberSaveable { mutableStateOf<String?>(null) }
    var coveredSuggestion by remember { mutableStateOf<CommuneCoverageResult.Covered?>(null) }

    LaunchedEffect(query) {
        coverageError = null
        val trimmed = query.trim()
        val ready = GeoApiGouvClient.isPostalCode(trimmed) || trimmed.length >= 2
        if (!ready) {
            suggestions = emptyList()
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        searching = true
        searchError = null
        delay(320)
        val result = withContext(Dispatchers.IO) {
            runCatching { GeoApiGouvClient.search(trimmed) }
        }
        searching = false
        result.onSuccess { suggestions = it }
            .onFailure {
                suggestions = emptyList()
                searchError = "Recherche indisponible. Réessayez dans un instant."
            }
    }

    fun resolveGeo(geo: GeoCommune, asLocationSuggestion: Boolean) {
        scope.launch {
            resolving = true
            coverageError = null
            if (!asLocationSuggestion) coveredSuggestion = null
            val result = withContext(Dispatchers.IO) {
                indexRepository.ensureLoaded()
                CommuneCoverageResolver.resolve(
                    geo = geo,
                    indexLookup = { insee -> indexRepository.findByInsee(insee) }
                )
            }
            resolving = false
            when (result) {
                is CommuneCoverageResult.Covered -> {
                    if (asLocationSuggestion || result.needsStreet) {
                        coveredSuggestion = result
                    } else {
                        onCommuneChosen(result.commune)
                    }
                }
                is CommuneCoverageResult.NotCovered -> {
                    coveredSuggestion = null
                    coverageError = CommuneCoverageResolver.notCoveredMessage(result)
                }
            }
        }
    }

    fun applyLocation() {
        scope.launch {
            locating = true
            locationError = null
            coverageError = null
            coveredSuggestion = null
            val location = locationHelper.readBestLastKnownLocation()
            if (location == null) {
                locating = false
                locationError =
                    "Position introuvable. Activez la localisation ou recherchez votre commune."
                return@launch
            }
            val geo = withContext(Dispatchers.IO) {
                runCatching {
                    GeoApiGouvClient.communeAt(location.latitude, location.longitude)
                }.getOrNull()
            }
            locating = false
            if (geo == null) {
                locationError = "Impossible d’identifier la commune à votre position."
                return@launch
            }
            query = geo.nom
            resolveGeo(geo, asLocationSuggestion = true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            applyLocation()
        } else {
            locationError =
                "Permission refusée. Vous pouvez rechercher votre commune manuellement."
        }
    }

    fun requestLocation() {
        locationError = null
        if (locationHelper.hasLocationPermission()) {
            applyLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val busy = locating || resolving
    val trimmedQuery = query.trim()
    val hasQuery = GeoApiGouvClient.isPostalCode(trimmedQuery) || trimmedQuery.length >= 2

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding
    ) {
        if (intro != null) {
            item(key = "intro") { intro() }
        }

        item(key = "location_btn") {
            OutlinedButton(
                onClick = { requestLocation() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Identification de la commune…")
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "search_field") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Commune ou code postal") },
                placeholder = { Text("Ex. 95420, Magny-en-Vexin…") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searching || resolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
            if (resolving) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recherche du calendrier (mairie, intercommunalité, opérateurs)…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            searchError?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            coverageError?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        coveredSuggestion?.let { match ->
            item(key = "geo_suggestion") {
                Spacer(modifier = Modifier.height(12.dp))
                CoveredSuggestionCard(
                    covered = match,
                    onConfirm = { commune -> onCommuneChosen(commune) }
                )
            }
        }

        if (hasQuery && suggestions.isNotEmpty()) {
            item(key = "suggestions_header") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (GeoApiGouvClient.isPostalCode(trimmedQuery)) {
                        "Communes du $trimmedQuery"
                    } else {
                        "Suggestions"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            geoSuggestionItems(
                suggestions = suggestions,
                showPostalCode = !GeoApiGouvClient.isPostalCode(trimmedQuery),
                enabled = !busy,
                onClick = { geo -> resolveGeo(geo, asLocationSuggestion = false) }
            )
        }

        if (hasQuery && !searching && suggestions.isEmpty() && searchError == null) {
            item(key = "no_suggestions") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucune commune trouvée pour « ${query.trim()} ».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "privacy") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "La recherche et la localisation utilisent geo.api.gouv.fr (service public). " +
                    "Aucune donnée n’est stockée sur nos serveurs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CoveredSuggestionCard(
    covered: CommuneCoverageResult.Covered,
    onConfirm: (VexinCommune) -> Unit
) {
    var street by remember(covered.commune.slug) { mutableStateOf("") }
    val streetReady = !covered.needsStreet || street.trim().length >= 3
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Commune identifiée",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = covered.commune.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Source : ${covered.sourceLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            if (covered.needsStreet) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Les jours changent selon la rue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Votre rue") },
                    placeholder = { Text("Ex. boulevard Émile Romanet") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val chosen = if (covered.needsStreet) {
                        covered.commune.copy(streetHint = street.trim())
                    } else {
                        covered.commune
                    }
                    onConfirm(chosen)
                },
                enabled = streetReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuer avec ${covered.commune.displayName}")
            }
        }
    }
}

private fun LazyListScope.geoSuggestionItems(
    suggestions: List<GeoCommune>,
    showPostalCode: Boolean,
    enabled: Boolean,
    onClick: (GeoCommune) -> Unit
) {
    items(suggestions, key = { it.codeInsee }) { geo ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick(geo) }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = geo.nom,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showPostalCode && geo.subtitle.isNotBlank()) {
                    Text(
                        text = geo.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

package com.collectes.app.data

import okhttp3.Request

sealed class CommuneCoverageResult {
    data class Covered(
        val commune: VexinCommune,
        /** Libellé court pour l’UI (ex. « SMIRTOM du Vexin »). */
        val sourceLabel: String
    ) : CommuneCoverageResult()

    data class NotCovered(val geoName: String) : CommuneCoverageResult()
}

/**
 * Résout une commune geo.api.gouv vers une source de calendrier supportée.
 * Cascade : index national → catalogue local → SMIRTOM (règles / fiche live).
 */
object CommuneCoverageResolver {

    fun resolve(
        geo: GeoCommune,
        indexLookup: (String) -> IndexedCommune? = { null },
        smirtomPageExists: (String) -> Boolean = ::probeSmirtomPage
    ): CommuneCoverageResult {
        indexLookup(geo.codeInsee)?.let { indexed ->
            return CommuneCoverageResult.Covered(
                commune = indexed.toVexinCommune().withGeoFallback(geo),
                sourceLabel = indexed.sourceLabel()
            )
        }

        matchCatalog(geo)?.let { commune ->
            return CommuneCoverageResult.Covered(commune, sourceLabelFor(commune))
        }

        val slug = VexinCommunes.nameToSlug(geo.nom)
        if (OfficialCommuneSchedules.weekdaysFor(slug) != null || smirtomPageExists(slug)) {
            return CommuneCoverageResult.Covered(
                commune = smirtomDiscovered(geo, slug),
                sourceLabel = "SMIRTOM du Vexin"
            )
        }

        return CommuneCoverageResult.NotCovered(geo.nom)
    }

    fun matchCatalog(geo: GeoCommune): VexinCommune? {
        val slug = VexinCommunes.nameToSlug(geo.nom)
        VexinCommunes.bySlug(slug)?.let { return it }
        val target = CalendarPdfMatcher.normalize(geo.nom)
        return VexinCommunes.all.find {
            CalendarPdfMatcher.normalize(it.displayName) == target
        }
    }

    fun smirtomDiscovered(geo: GeoCommune, slug: String = VexinCommunes.nameToSlug(geo.nom)): VexinCommune {
        return VexinCommune(
            slug = slug,
            displayName = geo.nom,
            officialCalendarUrl = "",
            infoPageUrl = null,
            latitude = geo.latitude,
            longitude = geo.longitude
        )
    }

    fun sourceLabelFor(commune: VexinCommune): String = when {
        commune.usesSmirtomNetwork -> "SMIRTOM du Vexin"
        commune.usesEmeraudeCalendarSource -> "Syndicat Emeraude"
        commune.usesSannoisMunicipalSource -> "Ville de Sannois"
        commune.usesNcpaCalendarSource -> "Normandie Cabourg Pays d’Auge"
        commune.usesThelloiseCalendarSource -> "Thelloise"
        commune.usesCcvtCalendarSource -> "CCVT"
        else -> "Calendrier officiel"
    }

    fun notCoveredMessage(geoName: String): String =
        "Calendrier introuvable pour $geoName pour le moment. " +
            "Cette commune n’est pas encore dans l’index Collectes."

    private fun VexinCommune.withGeoFallback(geo: GeoCommune): VexinCommune {
        if (latitude != 0.0 || longitude != 0.0) return this
        return copy(latitude = geo.latitude, longitude = geo.longitude)
    }

    internal fun probeSmirtomPage(slug: String): Boolean {
        if (slug.isBlank()) return false
        val url = "https://smirtomduvexin.net/informations_utiles/$slug/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .header("Accept", "text/html")
            .get()
            .build()
        return runCatching {
            SmirtomHttp.client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }
}

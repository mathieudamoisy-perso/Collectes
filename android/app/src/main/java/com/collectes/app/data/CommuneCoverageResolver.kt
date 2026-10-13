package com.collectes.app.data

import okhttp3.Request

sealed class CommuneCoverageResult {
    data class Covered(
        val commune: VexinCommune,
        /** Libellé court pour l’UI (ex. « SMIRTOM du Vexin »). */
        val sourceLabel: String,
        /** L’utilisateur doit indiquer sa rue avant de continuer. */
        val needsStreet: Boolean = false
    ) : CommuneCoverageResult()

    data class NotCovered(
        val geoName: String,
        val detail: String? = null
    ) : CommuneCoverageResult()
}

/**
 * Résout une commune geo.api.gouv vers une source de calendrier supportée.
 * Cascade : index → catalogue → SMIRTOM → découverte multi-sources
 * (mairie, EPCI, opérateurs liés).
 */
object CommuneCoverageResolver {

    fun resolve(
        geo: GeoCommune,
        indexLookup: (String) -> IndexedCommune? = { null },
        smirtomPageExists: (String) -> Boolean = ::probeSmirtomPage,
        discoverCalendar: (GeoCommune) -> DiscoveredCalendar? = { geo ->
            CommuneCalendarDiscovery.discover(
                geo = geo,
                fetchText = { url -> runCatching { SmirtomHttp.get(url) }.getOrNull() }
            )
        },
        openDataLookup: (GeoCommune) -> DiscoveredCalendar? = OpenDataScheduleLookup::find
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

        val discovered = runCatching { discoverCalendar(geo) }.getOrNull()
            ?: runCatching { openDataLookup(geo) }.getOrNull()
        if (discovered != null) {
            return CommuneCoverageResult.Covered(
                commune = discoveredCommune(geo, discovered),
                sourceLabel = discovered.sourceLabel,
                needsStreet = discovered.needsStreet
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

    fun discoveredCommune(geo: GeoCommune, discovered: DiscoveredCalendar): VexinCommune {
        return VexinCommune(
            slug = VexinCommunes.nameToSlug(geo.nom),
            displayName = geo.nom,
            officialCalendarUrl = discovered.calendarUrl,
            infoPageUrl = discovered.infoPageUrl,
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

    fun notCoveredMessage(result: CommuneCoverageResult.NotCovered): String {
        result.detail?.let { return it }
        return "Calendrier introuvable pour ${result.geoName} pour le moment. " +
            "Aucun PDF de collecte n’a été trouvé sur les sites liés à la commune " +
            "(mairie, intercommunalité, opérateurs)."
    }

    /** @deprecated Préférer [notCoveredMessage] avec [CommuneCoverageResult.NotCovered]. */
    fun notCoveredMessage(geoName: String): String =
        notCoveredMessage(CommuneCoverageResult.NotCovered(geoName))

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

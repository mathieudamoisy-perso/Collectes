package com.collectes.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Cherche un jeu « jours de collecte » sur data.gouv.fr (souvent un export OpenDataSoft).
 * Les jours y sont par rue, pas dans un PDF communal.
 */
object OpenDataScheduleLookup {
    private const val DATA_GOUV = "https://www.data.gouv.fr/api/1/datasets/"

    fun find(
        geo: GeoCommune,
        fetchText: (String) -> String? = { url -> runCatching { SmirtomHttp.get(url) }.getOrNull() }
    ): DiscoveredCalendar? {
        val query = "jours collecte déchets ${geo.nom}"
        val url = DATA_GOUV.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page_size", "5")
            .build()
        val body = fetchText(url.toString()) ?: return null
        val export = firstCollecteExport(body, geo.nom) ?: return null
        val records = toRecordsUrl(export) ?: return null
        return DiscoveredCalendar(
            calendarUrl = records,
            infoPageUrl = export,
            sourceLabel = "Données ouvertes",
            needsStreet = true
        )
    }

    internal fun firstCollecteExport(json: String, communeName: String): String? {
        val commune = CalendarPdfMatcher.normalize(communeName)
        val urls = Regex(""""url"\s*:\s*"(https?:[^"]+)"""")
            .findAll(json)
            .map { it.groupValues[1].replace("\\/", "/") }
            .filter { it.contains("/exports/", ignoreCase = true) || it.endsWith(".csv", true) || it.endsWith(".json", true) }
            .toList()
        if (urls.isEmpty()) return null
        val titles = Regex(""""title"\s*:\s*"([^"]+)"""")
            .findAll(json)
            .map { CalendarPdfMatcher.normalize(it.groupValues[1]) }
            .toList()
        val aboutCommune = titles.any { it.contains(commune) || commune.split(" ").any { token -> token.length >= 5 && it.contains(token) } }
        val aboutCollecte = titles.any { it.contains("collect") || it.contains("dechet") || it.contains("jour") }
        if (!aboutCommune || !aboutCollecte) return null
        return urls.firstOrNull { it.contains("/exports/", ignoreCase = true) } ?: urls.first()
    }

    internal fun toRecordsUrl(exportUrl: String): String? {
        val marker = "/exports/"
        val index = exportUrl.indexOf(marker)
        if (index < 0) return null
        return exportUrl.substring(0, index) + "/records"
    }
}

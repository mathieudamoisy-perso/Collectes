package com.collectes.app.data

import org.jsoup.nodes.Document
import java.time.LocalDate
import java.util.ArrayDeque
import java.util.Locale

data class DiscoveredCalendar(
    val calendarUrl: String,
    val infoPageUrl: String,
    val sourceLabel: String = "Site de la mairie",
    /** Les jours dépendent de la rue (fichier ouvert). */
    val needsStreet: Boolean = false
)

/**
 * Découverte automatique d’un calendrier PDF en cherchant plusieurs sources
 * liées à la commune : mairie, intercommunalité (EPCI), puis opérateurs
 * (syndicat / SICTOM / etc.) découverts via les liens.
 */
object CommuneCalendarDiscovery {
    private val PAGE_HINTS = listOf(
        "dechet", "déchet", "collecte", "calendrier", "proprete", "propreté",
        "tri", "ordure", "encombrant", "poubelle", "bac", "memo", "mémo",
        "environnement", "quotidien", "ramassage", "sictom", "syndicat"
    )
    /** Un PDF n’est un calendrier que s’il porte un de ces mots (pas seulement l’année). */
    private val PDF_REQUIRED = listOf(
        "calendrier", "collecte", "ordure", "ramassage", "memo", "mémo", "poubelle"
    )
    private val PDF_NOISE = listOf(
        "rapport", "organigramme", "magazine", "patrimoine", "architecte",
        "ccas", "fournisseur", "politique", "construction", "journal"
    )
    private val OPERATOR_HOST_HINTS = listOf(
        "tri-or", "smirtom", "emeraude", "sictom", "syndicat", "ccvt",
        "thelloise", "vexin", "collecte", "dechet", "ordure", "agglo",
        "metropole", "communaut", "siom", "sinova", "semardel", "syctom",
        "valor", "triord", "tri-vert", "ecotri"
    )
    /** Chemins fréquents à sonder même s’ils ne sont pas sur la page d’accueil. */
    private val PATH_PROBES = listOf(
        "/dechets",
        "/les-dechets",
        "/dechets/",
        "/collecte",
        "/collectes",
        "/calendrier",
        "/calendriers",
        "/vie-pratique/dechets",
        "/vie-pratique/environnement",
        "/cadre-de-vie/proprete",
        "/cadre-de-vie/la-proprete",
        "/environnement/dechets",
        "/mon-quotidien/preserve-mon-environnement/les-dechets",
        "/mon-quotidien/preserve-mon-environnement/les-dechets/la-collecte-des-dechets"
    )
    private const val MAX_PAGES_PER_SEED = 20
    private const val MAX_DEPTH = 2
    private const val MAX_LINKED_OPERATORS = 4
    private val LOC_PATTERN = Regex("<loc>\\s*([^<\\s]+)\\s*</loc>", RegexOption.IGNORE_CASE)
    private val SOURCE_URL = Regex(
        """"source_url"\s*:\s*"(https?:[^"]+\.pdf)"""",
        RegexOption.IGNORE_CASE
    )

    private data class Seed(
        val url: String,
        val label: String
    )

    fun discover(
        geo: GeoCommune,
        year: Int = LocalDate.now().year,
        websiteLookup: (String) -> String? = OfficialWebsiteLookup::websiteForInsee,
        epciWebsiteLookup: (String) -> String? = OfficialWebsiteLookup::websiteForEpci,
        fetchDocument: (String) -> Document? = ::safeDocument,
        fetchText: ((String) -> String?)? = null
    ): DiscoveredCalendar? {
        val primarySeeds = collectPrimarySeeds(geo, websiteLookup, epciWebsiteLookup)
        if (primarySeeds.isEmpty()) return null

        val commune = VexinCommune(
            slug = VexinCommunes.nameToSlug(geo.nom),
            displayName = geo.nom,
            infoPageUrl = primarySeeds.first().url
        )
        val candidates = mutableListOf<ScoredPdf>()
        val htmlSchedules = mutableListOf<String>()
        val linkedOperators = linkedSetOf<String>()

        primarySeeds.forEach { seed ->
            crawlSeed(
                seedUrl = seed.url,
                commune = commune,
                year = year,
                fetchDocument = fetchDocument,
                fetchText = fetchText,
                out = candidates,
                htmlSchedules = htmlSchedules,
                linkedOperatorsOut = linkedOperators
            )
        }

        // 2ᵉ vague : syndicats / opérateurs découverts depuis mairie ou EPCI.
        linkedOperators
            .asSequence()
            .mapNotNull { host -> absoluteHome(host) }
            .filter { home -> primarySeeds.none { hostOf(it.url) == hostOf(home) } }
            .take(MAX_LINKED_OPERATORS)
            .forEach { operatorHome ->
                crawlSeed(
                    seedUrl = operatorHome,
                    commune = commune,
                    year = year,
                    fetchDocument = fetchDocument,
                    fetchText = fetchText,
                    out = candidates,
                    htmlSchedules = htmlSchedules,
                    linkedOperatorsOut = linkedSetOf()
                )
            }

        return pick(candidates, htmlSchedules, primarySeeds, primarySeeds.first().url)
    }

    fun discoverFromWebsite(
        website: String,
        commune: VexinCommune,
        year: Int,
        fetchDocument: (String) -> Document? = ::safeDocument,
        fetchText: ((String) -> String?)? = null
    ): DiscoveredCalendar? {
        val candidates = mutableListOf<ScoredPdf>()
        val htmlSchedules = mutableListOf<String>()
        crawlSeed(
            seedUrl = website,
            commune = commune,
            year = year,
            fetchDocument = fetchDocument,
            fetchText = fetchText,
            out = candidates,
            htmlSchedules = htmlSchedules,
            linkedOperatorsOut = linkedSetOf()
        )
        return pick(
            candidates,
            htmlSchedules,
            listOf(Seed(website, "Site de la mairie")),
            website
        )
    }

    private fun pick(
        candidates: List<ScoredPdf>,
        htmlSchedules: List<String>,
        seeds: List<Seed>,
        fallbackInfo: String
    ): DiscoveredCalendar? {
        val best = candidates
            .distinctBy { it.url.lowercase(Locale.FRENCH) }
            .maxByOrNull { it.score }
        val infoPage = htmlSchedules.firstOrNull()
            ?: best?.foundOn?.takeIf { it.isNotBlank() }
            ?: best?.let { pdf -> seeds.firstOrNull { hostOf(it.url) == hostOf(pdf.url) }?.url }
            ?: fallbackInfo
        if (best != null) {
            return DiscoveredCalendar(
                calendarUrl = best.url,
                infoPageUrl = infoPage,
                sourceLabel = sourceLabelForUrl(best.url, seeds)
            )
        }
        val page = htmlSchedules.firstOrNull() ?: return null
        return DiscoveredCalendar(
            calendarUrl = "",
            infoPageUrl = page,
            sourceLabel = "Page de la commune"
        )
    }

    private fun collectPrimarySeeds(
        geo: GeoCommune,
        websiteLookup: (String) -> String?,
        epciWebsiteLookup: (String) -> String?
    ): List<Seed> {
        val seeds = linkedMapOf<String, Seed>()
        fun add(url: String?, label: String) {
            val clean = url?.trim()?.takeIf { it.isNotBlank() } ?: return
            val key = hostOf(clean) ?: normalizeUrl(clean)
            if (key !in seeds) seeds[key] = Seed(clean, label)
        }
        add(websiteLookup(geo.codeInsee), "Site de la mairie")
        geo.codeEpci
            ?.takeIf { it.isNotBlank() }
            ?.let { add(epciWebsiteLookup(it), "Intercommunalité") }
        return seeds.values.toList()
    }

    private fun crawlSeed(
        seedUrl: String,
        commune: VexinCommune,
        year: Int,
        fetchDocument: (String) -> Document?,
        fetchText: ((String) -> String?)?,
        out: MutableList<ScoredPdf>,
        htmlSchedules: MutableList<String>,
        linkedOperatorsOut: MutableSet<String>
    ) {
        val originHost = hostOf(seedUrl) ?: return
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(normalizeUrl(seedUrl) to 0)

        PATH_PROBES.forEach { path ->
            absoluteOnHost(seedUrl, path)?.let { queue.addLast(normalizeUrl(it) to 1) }
        }
        if (fetchText != null) {
            sitemapTargets(seedUrl, fetchText).forEach { loc ->
                if (loc.contains(".pdf", ignoreCase = true)) {
                    scorePdfCandidate(loc, loc, year, commune)?.let { out += it }
                } else {
                    queue.addFirst(normalizeUrl(loc) to 1)
                }
            }
            out += wordpressPdfs(originHost, commune, year, fetchText)
        }

        while (queue.isNotEmpty() && visited.size < MAX_PAGES_PER_SEED) {
            val (url, depth) = queue.removeFirst()
            val normalized = normalizeUrl(url)
            if (!visited.add(normalized)) continue

            val document = fetchDocument(normalized) ?: continue
            collectPdfs(document, year, commune, normalized, out)
            rememberHtmlSchedule(document, normalized, year, commune, htmlSchedules)

            if (depth >= MAX_DEPTH) continue
            document.select("a[href]").forEach { link ->
                val href = link.absUrl("href").ifBlank { return@forEach }
                if (href.startsWith("mailto:", ignoreCase = true)) return@forEach
                if (href.startsWith("tel:", ignoreCase = true)) return@forEach
                val host = hostOf(href) ?: return@forEach
                val haystack = buildString {
                    append(link.text()).append(' ')
                    append(href)
                }.lowercase(Locale.FRENCH)

                val namesCommune = linkNamesCommune(link.text(), commune)
                if (host == originHost) {
                    val next = normalizeUrl(href) to (depth + 1)
                    when {
                        isInterestingPage(href, haystack) || namesCommune -> queue.addFirst(next)
                        depth == 0 && isShallowPath(href) -> queue.addLast(next)
                    }
                } else if (looksLikeOperatorHost(host) || isInterestingPage(href, haystack) || namesCommune) {
                    linkedOperatorsOut += host
                    // Page externe intéressante : récupérer ses PDF tout de suite.
                    if (isInterestingPage(href, haystack) || href.contains(".pdf", ignoreCase = true)) {
                        if (href.contains(".pdf", ignoreCase = true)) {
                            scorePdfCandidate(href, link.text(), year, commune)?.let { out += it }
                        } else if (visited.size < MAX_PAGES_PER_SEED && visited.add(normalizeUrl(href))) {
                            fetchDocument(normalizeUrl(href))?.let { externalDoc ->
                                val page = normalizeUrl(href)
                                collectPdfs(externalDoc, year, commune, page, out)
                                rememberHtmlSchedule(externalDoc, page, year, commune, htmlSchedules)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun collectPdfs(
        document: Document,
        year: Int,
        commune: VexinCommune,
        foundOn: String,
        out: MutableList<ScoredPdf>
    ) {
        document.select("a[href]").forEach { link ->
            val href = link.absUrl("href").ifBlank { return@forEach }
            scorePdfCandidate(href, link.text(), year, commune, foundOn)?.let { out += it }
        }
    }

    private fun rememberHtmlSchedule(
        document: Document,
        pageUrl: String,
        year: Int,
        commune: VexinCommune,
        htmlSchedules: MutableList<String>
    ) {
        if (pageUrl in htmlSchedules) return
        val text = document.text()
        if (ProseScheduleParser.parseIfPresent(text, year, commune.displayName) != null) {
            htmlSchedules += pageUrl
        }
    }

    private fun scorePdfCandidate(
        href: String,
        linkText: String,
        year: Int,
        commune: VexinCommune,
        foundOn: String = ""
    ): ScoredPdf? {
        if (!href.contains(".pdf", ignoreCase = true)) return null
        val normalized = CalendarPdfMatcher.normalize("$linkText $href")
        if (PDF_NOISE.any { normalized.contains(it) }) return null
        val waste = PDF_REQUIRED.any { normalized.contains(CalendarPdfMatcher.normalize(it)) }
        val namedOnWastePage = isInterestingPage(foundOn, foundOn) &&
            linkNamesCommune(linkText, commune)
        if (!waste && !namedOnWastePage) return null
        var score = 30
        if (normalized.contains(year.toString())) score += 40
        if (normalized.contains((year - 1).toString())) score += 5
        if (CalendarPdfMatcher.matchesCommune(normalized, commune)) score += 35
        if (normalized.contains("calendrier")) score += 15
        val hasYear = normalized.contains(year.toString()) || normalized.contains((year - 1).toString())
        if (!hasYear && !namedOnWastePage) return null
        return ScoredPdf(url = href, score = score, foundOn = foundOn)
    }

    private fun sitemapTargets(seedUrl: String, fetchText: (String) -> String?): List<String> {
        val origin = hostOf(seedUrl) ?: return emptyList()
        val roots = listOf(
            "https://$origin/sitemap.xml",
            "https://www.$origin/sitemap.xml",
            "https://$origin/sitemap_index.xml"
        )
        val pages = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        fun pull(url: String, depth: Int) {
            if (!seen.add(url) || depth > 1) return
            val body = fetchText(url) ?: return
            val locs = LOC_PATTERN.findAll(body).map { it.groupValues[1].trim() }.toList()
            if (depth == 0) {
                locs.filter { it.endsWith(".xml", ignoreCase = true) }
                    .filter { loc ->
                        val n = loc.lowercase(Locale.FRENCH)
                        n.contains("dechet") || n.contains("collect") || n.contains("page") || n.contains("sitemap")
                    }
                    .take(4)
                    .forEach { pull(it, 1) }
            }
            pages += locs.filter { !it.endsWith(".xml", ignoreCase = true) }
        }
        roots.forEach { pull(it, 0) }
        return pages
            .filter { isInterestingPage(it, it) || it.contains(".pdf", ignoreCase = true) }
            .distinct()
            .take(25)
    }

    private fun wordpressPdfs(
        host: String,
        commune: VexinCommune,
        year: Int,
        fetchText: (String) -> String?
    ): List<ScoredPdf> {
        val query = java.net.URLEncoder.encode("calendrier ${commune.displayName}", Charsets.UTF_8.name())
        val out = mutableListOf<ScoredPdf>()
        listOf("https://$host", "https://www.$host").forEach { base ->
            val body = fetchText("$base/wp-json/wp/v2/media?search=$query&per_page=10") ?: return@forEach
            SOURCE_URL.findAll(body).forEach { match ->
                val pdf = match.groupValues[1].replace("\\/", "/")
                scorePdfCandidate(pdf, pdf, year, commune)?.let { out += it }
            }
        }
        return out
    }

    private fun linkNamesCommune(linkText: String, commune: VexinCommune): Boolean {
        val normalized = CalendarPdfMatcher.normalize(linkText)
        if (normalized.length < 4) return false
        if (CalendarPdfMatcher.matchesCommune(normalized, commune)) return true
        val token = CalendarPdfMatcher.normalize(commune.displayName)
            .split(" ")
            .firstOrNull { it.length >= 5 }
            ?: return false
        return normalized.contains(token)
    }

    private fun isInterestingPage(url: String, haystack: String): Boolean {
        val blob = CalendarPdfMatcher.normalize("$url $haystack")
        return PAGE_HINTS.any { blob.contains(CalendarPdfMatcher.normalize(it)) }
    }

    private fun looksLikeOperatorHost(host: String): Boolean {
        val h = host.lowercase(Locale.FRENCH)
        return OPERATOR_HOST_HINTS.any { h.contains(it) }
    }

    private fun isShallowPath(url: String): Boolean {
        val path = runCatching { java.net.URI(url).path }.getOrNull().orEmpty()
        return path.count { it == '/' } <= 2
    }

    private fun sourceLabelForUrl(pdfUrl: String, seeds: List<Seed>): String {
        val host = hostOf(pdfUrl) ?: return "Calendrier officiel"
        seeds.firstOrNull { hostOf(it.url) == host }?.let { return it.label }
        return when {
            host.contains("tri-or") -> "Syndicat TRI-OR"
            host.contains("smirtom") -> "SMIRTOM du Vexin"
            host.contains("emeraude") -> "Syndicat Emeraude"
            host.contains("thelloise") -> "Thelloise"
            host.contains("vexinthelle") -> "CCVT"
            looksLikeOperatorHost(host) -> "Opérateur de collecte"
            else -> "Calendrier officiel"
        }
    }

    private fun absoluteHome(host: String): String? {
        val clean = host.trim().removePrefix("www.")
        if (clean.isBlank()) return null
        return "https://www.$clean/"
    }

    private fun absoluteOnHost(baseUrl: String, path: String): String? {
        val host = hostOf(baseUrl) ?: return null
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "https://$host$normalizedPath"
    }

    private fun hostOf(url: String): String? = runCatching {
        java.net.URI(url).host?.lowercase(Locale.FRENCH)?.removePrefix("www.")
    }.getOrNull()

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().substringBefore('#').substringBefore('?')
        return if (trimmed.endsWith("/")) trimmed.dropLast(1) else trimmed
    }

    private fun safeDocument(url: String): Document? = runCatching {
        SmirtomHttp.document(url)
    }.getOrNull()

    private data class ScoredPdf(val url: String, val score: Int, val foundOn: String = "")
}

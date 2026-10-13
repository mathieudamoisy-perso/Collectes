package com.collectes.app.data

import java.io.File

/**
 * Lit un calendrier découvert ou catalogue en essayant plusieurs formats,
 * du plus spécifique au plus générique.
 */
object CalendarEventsReader {

    fun read(
        year: Int,
        commune: VexinCommune,
        pdfFile: File?,
        pdfText: String?,
        pageText: String?
    ): List<CollectionDay>? {
        // 1. Grille visuelle CCVT (bitmap).
        pdfFile?.let { CcvtCalendarParser.parseIfPresent(it, year) }?.let { return it }

        // 2. Sources à règles catalogue (NCPA / Thelloise) détectées via l’URL.
        if (commune.usesNcpaCalendarSource || commune.usesThelloiseCalendarSource) {
            return eventsFromRules(year, OfficialCommuneSchedules.rules(year, commune.slug))
        }

        // 3. Format Emeraude / pavillons (Sannois, Ermont…).
        pdfText?.let { MunicipalCalendarParser.parseIfPresent(it, year) }
            ?.let { return eventsFromRules(year, it) }

        // 4. Texte libre (mémo EPCI, page mairie) : PDF puis page.
        val combined = listOfNotNull(pdfText, pageText).joinToString("\n")
        ProseScheduleParser.parseIfPresent(combined, year, commune.displayName)
            ?.let { return eventsFromRules(year, it) }
        pdfText?.let { ProseScheduleParser.parseIfPresent(it, year, commune.displayName) }
            ?.let { return eventsFromRules(year, it) }
        pageText?.let { ProseScheduleParser.parseIfPresent(it, year, commune.displayName) }
            ?.let { return eventsFromRules(year, it) }

        // 5. Catalogue / SMIRTOM connu : reconcile classique.
        val knownSmirtomOrCatalog = commune.usesSmirtomNetwork ||
            OfficialCommuneSchedules.weekdaysFor(commune.slug) != null
        if (knownSmirtomOrCatalog) {
            val rules = CalendarReconciler.reconcile(
                pdfText = pdfText,
                pageText = pageText,
                commune = commune,
                year = year
            )
            return eventsFromRules(year, rules)
        }

        // 6. Calendrier dessiné (icônes, peu de texte).
        pdfFile?.let { ImageCalendarParser.parseIfPresent(it, year) }?.let { return it }

        // 7. Dernier recours texte SMIRTOM-like si le PDF/page est assez riche.
        CollectionRulesParser.parseIfPresent(combined, year, commune.displayName)
            ?.takeIf { rulesLookConfident(it, combined) }
            ?.let { return eventsFromRules(year, it) }

        return null
    }

    private fun rulesLookConfident(rules: CollectionRules, source: String): Boolean {
        val normalized = CollectionRulesParser.normalizeSource(source)
        val hits = listOf("ordures", "emballages", "verre").count { normalized.contains(it) }
        if (hits < 2) return false
        // Évite le défaut lundi/mardi trop générique sans ancre texte.
        val mentionsOrdureDay = normalized.contains(frenchDay(rules.orduresDay))
        val mentionsEmballagesDay = normalized.contains(frenchDay(rules.emballagesDay))
        return mentionsOrdureDay && mentionsEmballagesDay
    }

    private fun frenchDay(day: java.time.DayOfWeek): String = when (day) {
        java.time.DayOfWeek.MONDAY -> "lundi"
        java.time.DayOfWeek.TUESDAY -> "mardi"
        java.time.DayOfWeek.WEDNESDAY -> "mercredi"
        java.time.DayOfWeek.THURSDAY -> "jeudi"
        java.time.DayOfWeek.FRIDAY -> "vendredi"
        java.time.DayOfWeek.SATURDAY -> "samedi"
        java.time.DayOfWeek.SUNDAY -> "dimanche"
    }

    private fun eventsFromRules(year: Int, rules: CollectionRules): List<CollectionDay> {
        val regular = CalendarDateGenerator.generate(year, rules, includeNextYearJanuary = true)
        val encombrants = if (rules.encombrantsMonthOrdinal != null) {
            CalendarDateGenerator.encombrantsDates(year, rules, includeNextYearJanuary = true)
                .map { date -> CollectionDay(date, listOf(WasteType.ENCOMBRANTS)) }
        } else {
            emptyList()
        }
        return CollectionDayMerger.merge(regular + encombrants)
    }
}

package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Fusionne la page commune SMIRTOM (jours + résumé) et le PDF annuel (grille A/B).
 * La page est prioritaire pour les jours de semaine (spécifique à la commune).
 * La grille PDF départage les groupes verre A/B et corrige un résumé de page périmé.
 */
object CalendarReconciler {
    fun reconcile(
        pdfText: String?,
        pageText: String?,
        commune: VexinCommune,
        year: Int
    ): CollectionRules {
        if (!commune.usesSmirtomNetwork) {
            pdfText?.let { MunicipalCalendarParser.parseIfPresent(it, year) }?.let { return it }
        }

        val pageRules = pageText?.let {
            CollectionRulesParser.parseIfPresent(it, year, commune.displayName)
        }
        val pdfRules = pdfText?.let {
            CollectionRulesParser.parseIfPresent(it, year, commune.displayName)
        }
        val catalog = OfficialCommuneSchedules.rules(year, commune.slug)

        val orduresDay = resolveWeekday(
            page = pageRules?.orduresDay,
            pdf = pdfRules?.orduresDay,
            catalog = catalog.orduresDay,
            weekday = { OfficialCommuneSchedules.weekdaysFor(commune.slug)?.orduresDay }
        )
        val emballagesDay = resolveWeekday(
            page = pageRules?.emballagesDay,
            pdf = pdfRules?.emballagesDay,
            catalog = catalog.emballagesDay,
            weekday = { OfficialCommuneSchedules.weekdaysFor(commune.slug)?.emballagesDay }
        )
        var verreDay = resolveWeekday(
            page = pageRules?.verreDay,
            pdf = pdfRules?.verreDay,
            catalog = catalog.verreDay,
            weekday = { OfficialCommuneSchedules.weekdaysFor(commune.slug)?.verreDay }
        )

        if (!pdfText.isNullOrBlank() && pdfRules != null && pdfRules.verreDay != verreDay) {
            val pageSpecifiesVerreChange = pageText?.let {
                CollectionRulesParser.verreChangeAnchor(
                    CollectionRulesParser.normalizeSource(it),
                    verreDay
                )
            } != null
            if (!pageSpecifiesVerreChange) {
                val pageHits = PdfGridMarkers.letterCount(pdfText, verreDay)
                val pdfHits = PdfGridMarkers.letterCount(pdfText, pdfRules.verreDay)
                if (pdfHits > pageHits) {
                    verreDay = pdfRules.verreDay
                }
            }
        }

        val emballagesAnchor = emballagesAnchor(
            pdfText = pdfText,
            year = year,
            emballagesDay = emballagesDay,
            pdfRules = pdfRules,
            verreDay = verreDay
        )
        val verreAnchor = verreAnchor(
            pdfText = pdfText,
            pageText = pageText,
            communeName = commune.displayName,
            year = year,
            verreDay = verreDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor
        )

        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreDay,
            verreAnchor = verreAnchor
        )
    }

    /**
     * La page commune est prioritaire. Sans page, un PDF mal lu (légende sans jour → lundi par défaut)
     * ne doit pas écraser le rythme officiel connu de la commune.
     */
    private fun resolveWeekday(
        page: DayOfWeek?,
        pdf: DayOfWeek?,
        catalog: DayOfWeek,
        weekday: () -> DayOfWeek?
    ): DayOfWeek {
        page?.let { return it }
        if (pdf != null) {
            val official = weekday()
            if (official != null && pdf != official) return official
            return pdf
        }
        return catalog
    }

    private fun emballagesAnchor(
        pdfText: String?,
        year: Int,
        emballagesDay: DayOfWeek,
        pdfRules: CollectionRules?,
        verreDay: DayOfWeek
    ): LocalDate {
        if (!pdfText.isNullOrBlank() && emballagesDay != verreDay) {
            return PdfGridMarkers.pickEmballagesAnchor(pdfText, year, emballagesDay)
        }
        if (pdfRules != null && pdfRules.emballagesDay == emballagesDay) {
            return pdfRules.emballagesAnchor
        }
        return CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
    }

    private fun verreAnchor(
        pdfText: String?,
        pageText: String?,
        communeName: String,
        year: Int,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate {
        val combinedText = listOfNotNull(pageText, pdfText).joinToString("\n")
        CollectionRulesParser.verreChangeAnchor(combinedText, verreDay)?.let { changeAnchor ->
            return CalendarDateGenerator.firstFourWeeklyOnOrAfter(year, verreDay, changeAnchor)
        }
        return PdfGridMarkers.pickVerreAnchor(
            pdfText = pdfText,
            communeName = communeName,
            year = year,
            verreDay = verreDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor
        )
    }
}

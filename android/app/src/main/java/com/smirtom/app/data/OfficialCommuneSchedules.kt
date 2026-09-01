package com.smirtom.app.data

import java.time.DayOfWeek

/**
 * Jours de collecte 2026 relevés sur les pages SMIRTOM
 * `informations_utiles/{slug}/`, croisés avec les PDF groupés
 * (actualités « Calendriers de collecte 2026 »).
 *
 * Dernier recours si PDF et page commune sont injoignables.
 */
object OfficialCommuneSchedules {
    data class Weekdays(
        val orduresDay: DayOfWeek,
        val emballagesDay: DayOfWeek,
        val verreDay: DayOfWeek,
        val verreGroupB: Boolean = false
    )

    private val weekdays: Map<String, Weekdays> = mapOf(
        "magny-en-vexin" to Weekdays(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY, verreGroupB = true
        ),
        "charmont" to Weekdays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY),
        "ambleville" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "la-chapelle-en-vexin" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "omerville" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "saint-gervais" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "clery-en-vexin" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "hodent" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "nucourt" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "genainville" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "arthies" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "banthelu" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "maudetour-en-vexin" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "wy-dit-joli-village" to Weekdays(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY),
        "themericourt" to Weekdays(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        "cormeilles-en-vexin" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
        "sannois" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.MONDAY)
    )

    fun weekdaysFor(slug: String): Weekdays? = weekdays[slug]

    fun rules(year: Int, communeSlug: String): CollectionRules {
        val days = weekdays[communeSlug] ?: weekdays.getValue("magny-en-vexin")
        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(
            year,
            1,
            days.emballagesDay
        )
        val verreAnchor = if (days.emballagesDay == days.verreDay) {
            emballagesAnchor.plusDays(if (days.verreGroupB) 21L else 7L)
        } else {
            CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, days.verreDay)
        }
        return CollectionRules(
            orduresDay = days.orduresDay,
            emballagesDay = days.emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = days.verreDay,
            verreAnchor = verreAnchor
        )
    }
}

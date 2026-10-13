package com.collectes.app.data

import java.time.DayOfWeek

/**
 * Jours de collecte par rue (API OpenDataSoft records).
 * Champs Nantes : libelle, bleu_jour_collecte (ordures), jaune_jour_collecte (emballages).
 */
object OpenDataCalendarParser {
    private val DAY_WORDS = mapOf(
        "lundi" to DayOfWeek.MONDAY,
        "mardi" to DayOfWeek.TUESDAY,
        "mercredi" to DayOfWeek.WEDNESDAY,
        "jeudi" to DayOfWeek.THURSDAY,
        "vendredi" to DayOfWeek.FRIDAY,
        "samedi" to DayOfWeek.SATURDAY,
        "dimanche" to DayOfWeek.SUNDAY
    )

    fun isRecordsUrl(url: String): Boolean = url.contains("/records", ignoreCase = true) &&
        url.contains("/api/explore/", ignoreCase = true)

    fun parse(
        recordsUrl: String,
        street: String,
        year: Int,
        fetchText: (String) -> String? = { url -> runCatching { SmirtomHttp.get(url) }.getOrNull() }
    ): List<CollectionDay>? {
        val query = street.trim()
        if (query.length < 3) return null
        val url = recordsUrl.substringBefore('?') + "?limit=15&where=" +
            java.net.URLEncoder.encode("""search(libelle,"$query")""", Charsets.UTF_8.name())
        val body = fetchText(url) ?: return null
        return parseRecords(body, query, year)
    }

    internal fun parseRecords(json: String, street: String, year: Int): List<CollectionDay>? {
        val objects = Regex("""\{[^{}]*"libelle"[^{}]*\}""").findAll(json).map { it.value }.toList()
        if (objects.isEmpty()) return null
        val target = CalendarPdfMatcher.normalize(street)
        val best = objects.maxByOrNull { scoreStreet(it, target) } ?: return null
        if (scoreStreet(best, target) <= 0) return null
        val ordures = weekdayField(best, listOf("bleu_jour_collecte", "jour_om", "ordures"))
        val emballages = weekdayField(best, listOf("jaune_jour_collecte", "jour_tri", "emballages"))
        if (ordures == null && emballages == null) return null
        val orduresDay = ordures ?: emballages!!
        val emballagesDay = emballages ?: orduresDay
        val anchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
        val rules = CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = anchor,
            verreDay = emballagesDay,
            verreAnchor = anchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = if (emballages != null) CollectionRecurrence.WEEKLY else CollectionRecurrence.BIWEEKLY,
            verreRecurrence = CollectionRecurrence.EVERY_FOUR_WEEKS
        )
        return CalendarDateGenerator.generate(year, rules, includeNextYearJanuary = true)
    }

    private fun scoreStreet(obj: String, target: String): Int {
        val label = CalendarPdfMatcher.normalize(stringField(obj, "libelle").orEmpty())
        if (label.isBlank() || target.isBlank()) return 0
        val tokens = target.split(" ").filter { it.length >= 3 }
        if (tokens.isEmpty()) return if (label.contains(target)) 1 else 0
        return tokens.count { label.contains(it) }
    }

    private fun weekdayField(obj: String, keys: List<String>): DayOfWeek? {
        for (key in keys) {
            val raw = stringField(obj, key) ?: continue
            val normalized = CalendarPdfMatcher.normalize(raw)
            DAY_WORDS.entries.firstOrNull { normalized.contains(it.key) }?.let { return it.value }
        }
        return null
    }

    private fun stringField(obj: String, key: String): String? {
        val regex = """"$key"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return regex.find(obj)?.groupValues?.get(1)
    }
}

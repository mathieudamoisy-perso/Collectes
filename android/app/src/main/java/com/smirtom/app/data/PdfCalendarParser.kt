package com.smirtom.app.data

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class PdfCalendarParser(
    private val resourceLoader: (() -> Unit)? = null
) {
    fun parse(pdfFile: File, year: Int): List<CollectionDay> {
        resourceLoader?.invoke()
        PDDocument.load(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            val rules = extractRules(text, year)
            return CalendarDateGenerator.generate(year, rules, includeNextYearJanuary = true)
        }
    }

    internal fun extractRules(text: String, year: Int): CollectionRules {
        val normalized = text.lowercase(Locale.FRENCH).replace('\u00a0', ' ')

        val orduresDay = findDayAfter(
            normalized,
            listOf("ordures ménagères", "ordures menageres", "ordures ménageres")
        ) ?: DayOfWeek.MONDAY

        val emballagesDay = findDayAfter(
            normalized,
            listOf("emballages / papiers", "emballages", "papiers")
        ) ?: DayOfWeek.TUESDAY

        val verreDay = findDayAfter(
            normalized,
            listOf("verre")
        ) ?: DayOfWeek.TUESDAY

        val emballagesAnchor = findAnchorDate(normalized, year, emballagesDay)
            ?: CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)

        val verreAnchor = findVerreAnchor(normalized, year, verreDay, emballagesAnchor)
            ?: emballagesAnchor

        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreDay,
            verreAnchor = verreAnchor
        )
    }

    private fun findDayAfter(text: String, keywords: List<String>): DayOfWeek? {
        for (keyword in keywords) {
            val index = text.indexOf(keyword)
            if (index >= 0) {
                val window = text.substring(index, (index + 120).coerceAtMost(text.length))
                DAY_PATTERN.findAll(window).firstOrNull()?.let { match ->
                    return frenchDayToDayOfWeek(match.value)
                }
            }
        }
        return null
    }

    private fun findAnchorDate(text: String, year: Int, dayOfWeek: DayOfWeek): LocalDate? {
        val monthMap = mapOf(
            "janv" to 1, "févr" to 2, "fevr" to 2, "mars" to 3, "avr" to 4,
            "mai" to 5, "juin" to 6, "juil" to 7, "août" to 8, "aout" to 8,
            "sept" to 9, "oct" to 10, "nov" to 11, "déc" to 12, "dec" to 12
        )

        val pattern = Regex("""(\d{1,2})\s*(janv|févr|fevr|mars|avr|mai|juin|juil|août|aout|sept|oct|nov|déc|dec)-?(\d{2})""")
        val matches = pattern.findAll(text).mapNotNull { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val month = monthMap[match.groupValues[2]] ?: return@mapNotNull null
            val yearSuffix = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
            val fullYear = 2000 + yearSuffix
            runCatching { LocalDate.of(fullYear, month, day) }.getOrNull()
        }.filter { it.year == year && it.dayOfWeek == dayOfWeek }
            .toList()

        return matches.minByOrNull { it.dayOfYear }
    }

    private fun findVerreAnchor(
        text: String,
        year: Int,
        verreDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate? {
        if (!text.contains("4 semaines")) {
            return emballagesAnchor
        }
        return findAnchorDate(text, year, verreDay) ?: emballagesAnchor
    }

    private fun frenchDayToDayOfWeek(day: String): DayOfWeek? {
        return when (day.lowercase(Locale.FRENCH)) {
            "lundi" -> DayOfWeek.MONDAY
            "mardi" -> DayOfWeek.TUESDAY
            "mercredi" -> DayOfWeek.WEDNESDAY
            "jeudi" -> DayOfWeek.THURSDAY
            "vendredi" -> DayOfWeek.FRIDAY
            "samedi" -> DayOfWeek.SATURDAY
            "dimanche" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    companion object {
        private val DAY_PATTERN = Regex(
            """\b(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\b""",
            RegexOption.IGNORE_CASE
        )
    }
}

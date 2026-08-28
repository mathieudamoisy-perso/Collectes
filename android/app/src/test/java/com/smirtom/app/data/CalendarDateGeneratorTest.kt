package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CalendarDateGeneratorTest {
  private val rules = CollectionRules(
    orduresDay = DayOfWeek.MONDAY,
    emballagesDay = DayOfWeek.TUESDAY,
    emballagesAnchor = LocalDate.of(2026, 1, 6),
    verreDay = DayOfWeek.TUESDAY,
    verreAnchor = LocalDate.of(2026, 1, 6)
  )

  @Test
  fun generatesWeeklyMondayOrdures() {
    val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
    val mondays = events.filter { it.wasteTypes.contains(WasteType.ORDURES) }
    assertTrue(mondays.any { it.date == LocalDate.of(2026, 1, 5) })
    assertTrue(mondays.any { it.date == LocalDate.of(2026, 1, 12) })
  }

  @Test
  fun generatesBiweeklyEmballages() {
    val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
    val emballages = events.filter { it.wasteTypes.contains(WasteType.EMBALLAGES) }
    assertTrue(emballages.any { it.date == LocalDate.of(2026, 1, 6) })
    assertEquals(false, emballages.any { it.date == LocalDate.of(2026, 1, 13) })
    assertTrue(emballages.any { it.date == LocalDate.of(2026, 1, 20) })
  }

  @Test
  fun reminderDayIsDayBeforeCollection() {
    val collectionDate = LocalDate.of(2026, 3, 3)
    val reminderDate = collectionDate.minusDays(1)
    assertEquals(LocalDate.of(2026, 3, 2), reminderDate)
  }
}

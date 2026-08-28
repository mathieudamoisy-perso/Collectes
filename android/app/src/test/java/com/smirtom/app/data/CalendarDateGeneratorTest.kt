package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    verreAnchor = LocalDate.of(2026, 1, 13)
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
    assertFalse(emballages.any { it.date == LocalDate.of(2026, 1, 13) })
    assertTrue(emballages.any { it.date == LocalDate.of(2026, 1, 20) })
  }

  @Test
  fun generatesFourWeeklyVerreOnAlternatingTuesdays() {
    val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
    val verre = events.filter { it.wasteTypes.contains(WasteType.VERRE) }
    assertTrue(verre.any { it.date == LocalDate.of(2026, 1, 13) })
    assertFalse(verre.any { it.date == LocalDate.of(2026, 1, 6) })
    assertTrue(verre.any { it.date == LocalDate.of(2026, 2, 10) })
  }

  @Test
  fun neverCombinesRegularBinsOnSameDay() {
    val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
    val overlapping = events.filter { day ->
      day.wasteTypes.filter { it != WasteType.ENCOMBRANTS }.size > 1
    }
    assertTrue(overlapping.isEmpty())
  }

  @Test
  fun neverCombinesOrduresAndEmballagesEvenIfSameWeekday() {
    val colliding = CollectionRules(
      orduresDay = DayOfWeek.TUESDAY,
      emballagesDay = DayOfWeek.TUESDAY,
      emballagesAnchor = LocalDate.of(2026, 1, 6),
      verreDay = DayOfWeek.TUESDAY,
      verreAnchor = LocalDate.of(2026, 1, 13)
    )
    val events = CalendarDateGenerator.generate(2026, colliding, includeNextYearJanuary = false)
    assertTrue(
      events.none {
        it.wasteTypes.contains(WasteType.ORDURES) &&
          (it.wasteTypes.contains(WasteType.EMBALLAGES) || it.wasteTypes.contains(WasteType.VERRE))
      }
    )
  }

  @Test
  fun reminderDayIsDayBeforeCollection() {
    val collectionDate = LocalDate.of(2026, 3, 3)
    val reminderDate = collectionDate.minusDays(1)
    assertEquals(LocalDate.of(2026, 3, 2), reminderDate)
  }

  @Test
  fun magnyGroupBIncludes8SeptemberVerre() {
    val magny = CollectionRules(
      orduresDay = DayOfWeek.MONDAY,
      emballagesDay = DayOfWeek.TUESDAY,
      emballagesAnchor = LocalDate.of(2026, 1, 6),
      verreDay = DayOfWeek.TUESDAY,
      verreAnchor = LocalDate.of(2026, 1, 27)
    )
    val events = CalendarDateGenerator.generate(2026, magny, includeNextYearJanuary = false)
    val verre = events.filter { it.wasteTypes.contains(WasteType.VERRE) }
    assertTrue(verre.any { it.date == LocalDate.of(2026, 1, 27) })
    assertTrue(verre.any { it.date == LocalDate.of(2026, 9, 8) })
    assertFalse(verre.any { it.date == LocalDate.of(2026, 1, 13) })
    assertFalse(
      events.any {
        it.date == LocalDate.of(2026, 9, 8) && it.wasteTypes.contains(WasteType.EMBALLAGES)
      }
    )
  }
}

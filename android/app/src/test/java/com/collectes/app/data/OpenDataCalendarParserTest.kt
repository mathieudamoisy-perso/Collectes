package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class OpenDataCalendarParserTest {
    @Test
    fun parsesNantesStreetRecord() {
        val json = """
            {"results":[{
              "libelle":"Impasse Emile Cossé",
              "bleu_jour_collecte":"Vendredi",
              "jaune_jour_collecte":"mardi"
            }]}
        """.trimIndent()
        val events = OpenDataCalendarParser.parseRecords(json, "cossé", 2026)
        assertNotNull(events)
        assertTrue(events!!.any {
            it.date == LocalDate.of(2026, 1, 2) && WasteType.ORDURES in it.wasteTypes
        })
        assertTrue(events.any {
            it.date == LocalDate.of(2026, 1, 6) && WasteType.EMBALLAGES in it.wasteTypes
        })
    }

    @Test
    fun ignoresColumnOnlyStreets() {
        val json = """
            {"results":[{
              "libelle":"Boulevard Emile Romanet",
              "bleu_jour_collecte":"en colonnes",
              "jaune_jour_collecte":"en colonnes"
            }]}
        """.trimIndent()
        assertEquals(null, OpenDataCalendarParser.parseRecords(json, "romanet", 2026))
    }

    @Test
    fun recordsUrlFromExport() {
        val export = "https://data.nantesmetropole.fr/api/explore/v2.1/catalog/datasets/244400404_jours-collectes-dechets-nantes/exports/csv?use_labels=true"
        assertTrue(OpenDataScheduleLookup.toRecordsUrl(export)!!.endsWith("/records"))
    }

    @Test
    fun weekdayMappingUsesFriday() {
        val json = """
            {"results":[{"libelle":"Rue de Paris","bleu_jour_collecte":"Vendredi","jaune_jour_collecte":"lundi"}]}
        """.trimIndent()
        val events = OpenDataCalendarParser.parseRecords(json, "paris", 2026)!!
        assertEquals(DayOfWeek.FRIDAY, events.first { WasteType.ORDURES in it.wasteTypes }.date.dayOfWeek)
    }
}

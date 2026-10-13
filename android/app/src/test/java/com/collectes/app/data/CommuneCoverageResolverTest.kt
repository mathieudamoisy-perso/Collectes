package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommuneCoverageResolverTest {
    @Test
    fun catalogMatchByName() {
        val geo = GeoCommune(
            nom = "Magny-en-Vexin",
            codeInsee = "95355",
            codeDepartement = "95",
            codesPostaux = listOf("95420"),
            latitude = 49.15,
            longitude = 1.78
        )
        val match = CommuneCoverageResolver.matchCatalog(geo)
        assertEquals("magny-en-vexin", match?.slug)
    }

    @Test
    fun resolveUsesCatalogWithoutSmirtomProbe() {
        val geo = GeoCommune(
            nom = "Sannois",
            codeInsee = "95582",
            codeDepartement = "95",
            codesPostaux = listOf("95110"),
            latitude = 48.97,
            longitude = 2.25
        )
        var probed = false
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            smirtomPageExists = {
                probed = true
                false
            },
            discoverCalendar = { null }
        )
        assertTrue(result is CommuneCoverageResult.Covered)
        assertEquals("sannois", (result as CommuneCoverageResult.Covered).commune.slug)
        assertEquals(false, probed)
    }

    @Test
    fun resolveDiscoversSmirtomFromKnownWeekdays() {
        val geo = GeoCommune(
            nom = "Arthies",
            codeInsee = "95023",
            codeDepartement = "95",
            codesPostaux = listOf("95420"),
            latitude = 49.1,
            longitude = 1.8
        )
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            smirtomPageExists = { false },
            discoverCalendar = { null }
        )
        assertTrue(result is CommuneCoverageResult.Covered)
        val covered = result as CommuneCoverageResult.Covered
        assertEquals("arthies", covered.commune.slug)
        assertTrue(covered.commune.usesSmirtomNetwork)
        assertEquals("SMIRTOM du Vexin", covered.sourceLabel)
    }

    @Test
    fun resolveDiscoversSmirtomViaProbe() {
        val geo = GeoCommune(
            nom = "Ville-inventée-test",
            codeInsee = "99999",
            codeDepartement = "95",
            codesPostaux = emptyList(),
            latitude = 0.0,
            longitude = 0.0
        )
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            smirtomPageExists = { slug -> slug == "ville-inventee-test" },
            discoverCalendar = { null }
        )
        assertTrue(result is CommuneCoverageResult.Covered)
        assertEquals("ville-inventee-test", (result as CommuneCoverageResult.Covered).commune.slug)
    }

    @Test
    fun resolveNotCoveredWhenUnknown() {
        val geo = GeoCommune(
            nom = "Paris",
            codeInsee = "75056",
            codeDepartement = "75",
            codesPostaux = listOf("75001"),
            latitude = 48.85,
            longitude = 2.35
        )
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            indexLookup = { null },
            smirtomPageExists = { false },
            discoverCalendar = { null },
            openDataLookup = { null }
        )
        assertTrue(result is CommuneCoverageResult.NotCovered)
        assertEquals("Paris", (result as CommuneCoverageResult.NotCovered).geoName)
    }

    @Test
    fun resolvePrefersNationalIndex() {
        val geo = GeoCommune(
            nom = "Paris",
            codeInsee = "75056",
            codeDepartement = "75",
            codesPostaux = listOf("75001"),
            latitude = 48.85,
            longitude = 2.35
        )
        val indexed = IndexedCommune(
            insee = "75056",
            slug = "paris",
            name = "Paris",
            source = "direct",
            calendarUrl = "https://example.com/paris.pdf",
            infoPageUrl = "https://example.com/info",
            lat = 48.85,
            lon = 2.35
        )
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            indexLookup = { if (it == "75056") indexed else null },
            smirtomPageExists = { false },
            discoverCalendar = { null }
        )
        assertTrue(result is CommuneCoverageResult.Covered)
        assertEquals("paris", (result as CommuneCoverageResult.Covered).commune.slug)
        assertEquals("https://example.com/paris.pdf", result.commune.officialCalendarUrl)
    }

    @Test
    fun resolveUsesDiscoveredCalendarFromMairie() {
        val geo = GeoCommune(
            nom = "Exemple-Ville",
            codeInsee = "99998",
            codeDepartement = "95",
            codesPostaux = listOf("95000"),
            latitude = 49.0,
            longitude = 2.0
        )
        val discovered = DiscoveredCalendar(
            calendarUrl = "https://mairie.example.fr/calendrier-2026-exemple-ville.pdf",
            infoPageUrl = "https://mairie.example.fr",
            sourceLabel = "Site de la mairie"
        )
        val result = CommuneCoverageResolver.resolve(
            geo = geo,
            indexLookup = { null },
            smirtomPageExists = { false },
            discoverCalendar = { discovered },
            openDataLookup = { null }
        )
        assertTrue(result is CommuneCoverageResult.Covered)
        val covered = result as CommuneCoverageResult.Covered
        assertEquals("exemple-ville", covered.commune.slug)
        assertEquals(discovered.calendarUrl, covered.commune.officialCalendarUrl)
        assertEquals(discovered.infoPageUrl, covered.commune.infoPageUrl)
        assertEquals("Site de la mairie", covered.sourceLabel)
        assertEquals(false, covered.commune.usesSmirtomNetwork)
    }
}

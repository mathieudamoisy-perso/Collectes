package com.collectes.app.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommuneCalendarDiscoveryTest {
    private val commune = VexinCommune(
        slug = "exemple-ville",
        displayName = "Exemple-Ville",
        infoPageUrl = "https://mairie.exemple.fr"
    )

    @Test
    fun findsCalendarPdfOnMairieSite() {
        val pages = mapOf(
            "https://mairie.exemple.fr" to """
                <html><body>
                  <a href="/cadre-de-vie/proprete/">Propreté</a>
                </body></html>
            """.trimIndent(),
            "https://mairie.exemple.fr/cadre-de-vie/proprete" to """
                <html><body>
                  <a href="/docs/calendrier-collecte-2026-exemple-ville.pdf">
                    Calendrier 2026 Exemple-Ville
                  </a>
                </body></html>
            """.trimIndent()
        )
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { url ->
                pages[url.substringBefore('?')]?.let { Jsoup.parse(it, url) }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("calendrier-collecte-2026-exemple-ville.pdf"))
        assertTrue(result.infoPageUrl.contains("mairie.exemple.fr"))
    }

    @Test
    fun followsSyndicatLinkFromMairie() {
        val pages = mapOf(
            "https://mairie.exemple.fr" to """
                <html><body>
                  <a href="https://syndicat-collecte.example/calendriers/">
                    Calendriers déchets
                  </a>
                </body></html>
            """.trimIndent(),
            "https://syndicat-collecte.example/calendriers" to """
                <html><body>
                  <a href="https://syndicat-collecte.example/Calendrier-2026-Exemple-Ville.pdf">
                    PDF
                  </a>
                </body></html>
            """.trimIndent()
        )
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { url ->
                pages[url.substringBefore('?')]?.let { Jsoup.parse(it, url) }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("Calendrier-2026-Exemple-Ville.pdf"))
    }

    @Test
    fun returnsNullWhenNoPdf() {
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { url ->
                Jsoup.parse("<html><body><a href='/contact/'>Contact</a></body></html>", url)
            }
        )
        assertNull(result)
    }

    @Test
    fun discoverUsesWebsiteLookupThenCrawl() {
        val geo = GeoCommune(
            nom = "Exemple-Ville",
            codeInsee = "99998",
            codeDepartement = "95",
            codesPostaux = listOf("95000"),
            latitude = 49.0,
            longitude = 2.0
        )
        val result = CommuneCalendarDiscovery.discover(
            geo = geo,
            year = 2026,
            websiteLookup = { "https://mairie.exemple.fr" },
            epciWebsiteLookup = { null },
            fetchDocument = { url ->
                when {
                    url.contains("mairie.exemple.fr") && !url.contains(".pdf") ->
                        Jsoup.parse(
                            """
                            <html><body>
                              <a href="/calendrier-2026-exemple-ville.pdf">Calendrier collecte 2026</a>
                            </body></html>
                            """.trimIndent(),
                            url
                        )
                    else -> null
                }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("calendrier-2026-exemple-ville.pdf"))
    }

    @Test
    fun discoverFallsBackToEpciWhenMairieHasNoPdf() {
        val geo = GeoCommune(
            nom = "Annecy",
            codeInsee = "74010",
            codeDepartement = "74",
            codesPostaux = listOf("74000"),
            latitude = 45.9,
            longitude = 6.1,
            codeEpci = "200066793"
        )
        val result = CommuneCalendarDiscovery.discover(
            geo = geo,
            year = 2026,
            websiteLookup = { "https://www.annecy.fr" },
            epciWebsiteLookup = { "https://www.grandannecy.fr" },
            fetchDocument = { url ->
                when {
                    url.contains("annecy.fr") && !url.contains("grandannecy") ->
                        Jsoup.parse("<html><body><a href='/contact/'>Contact</a></body></html>", url)
                    url.contains("grandannecy.fr") ->
                        Jsoup.parse(
                            """
                            <html><body>
                              <a href="/fileadmin/Memos-collecte-2026-Annecy.pdf">
                                ANNECY Jours de collecte 2026
                              </a>
                            </body></html>
                            """.trimIndent(),
                            url
                        )
                    else -> null
                }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("Memos-collecte-2026-Annecy.pdf"))
        assertEquals("Intercommunalité", result.sourceLabel)
    }

    @Test
    fun discoverCrawlsLinkedOperatorAsSecondWave() {
        val geo = GeoCommune(
            nom = "Exemple-Ville",
            codeInsee = "99998",
            codeDepartement = "95",
            codesPostaux = listOf("95000"),
            latitude = 49.0,
            longitude = 2.0
        )
        val result = CommuneCalendarDiscovery.discover(
            geo = geo,
            year = 2026,
            websiteLookup = { "https://mairie.exemple.fr" },
            epciWebsiteLookup = { null },
            fetchDocument = { url ->
                when {
                    url.contains("mairie.exemple.fr") ->
                        Jsoup.parse(
                            """
                            <html><body>
                              <a href="https://syndicat-dechets.example/">Syndicat déchets</a>
                            </body></html>
                            """.trimIndent(),
                            url
                        )
                    url.contains("syndicat-dechets.example") ->
                        Jsoup.parse(
                            """
                            <html><body>
                              <a href="/Calendrier-2026-Exemple-Ville.pdf">Calendrier 2026</a>
                            </body></html>
                            """.trimIndent(),
                            url
                        )
                    else -> null
                }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("Calendrier-2026-Exemple-Ville.pdf"))
        assertEquals("Opérateur de collecte", result.sourceLabel)
    }

    @Test
    fun ignoresMagazinePdfAndKeepsCollectePdf() {
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { url ->
                if (!url.contains("mairie.exemple.fr")) return@discoverFromWebsite null
                Jsoup.parse(
                    """
                    <html><body>
                      <a href="/journal-2026-exemple-ville.pdf">Journal municipal 2026</a>
                      <a href="/calendrier-collecte-2026-exemple-ville.pdf">Calendrier collecte 2026</a>
                    </body></html>
                    """.trimIndent(),
                    url
                )
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("calendrier-collecte-2026"))
        assertTrue(!result.calendarUrl.contains("journal"))
    }

    @Test
    fun acceptsHtmlPageWhenDaysAreWrittenWithoutPdf() {
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { url ->
                if (!url.endsWith("mairie.exemple.fr")) return@discoverFromWebsite null
                Jsoup.parse(
                    """
                    <html><body>
                      <h1>Exemple-Ville</h1>
                      <p>Ordures ménagères : lundi</p>
                      <p>Emballages : vendredi</p>
                    </body></html>
                    """.trimIndent(),
                    url
                )
            }
        )
        assertNotNull(result)
        assertEquals("", result!!.calendarUrl)
        assertTrue(result.infoPageUrl.contains("mairie.exemple.fr"))
    }

    @Test
    fun readsCalendarListedOnlyInSitemap() {
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://mairie.exemple.fr",
            commune = commune,
            year = 2026,
            fetchDocument = { null },
            fetchText = { url ->
                if (url.contains("sitemap.xml")) {
                    """
                    <urlset>
                      <url><loc>https://mairie.exemple.fr/docs/calendrier-collecte-2026-exemple-ville.pdf</loc></url>
                    </urlset>
                    """.trimIndent()
                } else {
                    null
                }
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("calendrier-collecte-2026-exemple-ville.pdf"))
    }

    @Test
    fun acceptsPdfNamedOnlyByCommuneOnWastePage() {
        val annecy = VexinCommune(slug = "annecy", displayName = "Annecy")
        val result = CommuneCalendarDiscovery.discoverFromWebsite(
            website = "https://www.grandannecy.fr",
            commune = annecy,
            year = 2026,
            fetchDocument = { url ->
                if (!url.contains("dechets")) return@discoverFromWebsite null
                Jsoup.parse(
                    """
                    <html><body>
                      <a href="/files/annecy-ville.pdf">Annecy</a>
                    </body></html>
                    """.trimIndent(),
                    url
                )
            }
        )
        assertNotNull(result)
        assertTrue(result!!.calendarUrl.contains("annecy-ville.pdf"))
    }
}

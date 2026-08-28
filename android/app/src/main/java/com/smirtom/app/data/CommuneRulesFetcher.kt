package com.smirtom.app.data

import org.jsoup.Jsoup

class CommuneRulesFetcher {
    fun fetchRules(commune: VexinCommune, year: Int): CollectionRules {
        val document = Jsoup.connect(commune.pageUrl)
            .userAgent("SmirtomApp/1.0")
            .timeout(30_000)
            .get()

        return CollectionRulesParser.parse(
            text = document.text(),
            year = year,
            communeName = commune.displayName
        )
    }
}

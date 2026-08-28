package com.smirtom.app.data

class CommuneRulesFetcher {
    fun fetchText(commune: VexinCommune): String {
        return SmirtomHttp.document(commune.pageUrl).text()
    }

    fun fetchRules(commune: VexinCommune, year: Int): CollectionRules {
        return CollectionRulesParser.parse(
            text = fetchText(commune),
            year = year,
            communeName = commune.displayName
        )
    }
}

package com.smirtom.app.data

class CommuneRulesFetcher {
    fun fetchRules(commune: VexinCommune, year: Int): CollectionRules {
        val document = SmirtomHttp.document(commune.pageUrl)
        return CollectionRulesParser.parse(
            text = document.text(),
            year = year,
            communeName = commune.displayName
        )
    }
}

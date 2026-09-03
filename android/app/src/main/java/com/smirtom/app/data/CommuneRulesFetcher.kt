package com.smirtom.app.data

class CommuneRulesFetcher {
    fun fetchText(commune: VexinCommune): String {
        return SmirtomHttp.document(commune.pageUrl).text()
    }
}

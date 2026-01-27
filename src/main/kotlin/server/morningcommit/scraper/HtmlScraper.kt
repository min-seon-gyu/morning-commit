package server.morningcommit.scraper

import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class HtmlScraper {

    companion object {
        private const val TIMEOUT_MS = 5000
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    fun scrapeContent(url: String): String {
        val document = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .get()

        return document.body().text()
    }
}

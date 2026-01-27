package server.morningcommit.scraper

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HtmlScraperTest {

    private val htmlScraper = HtmlScraper()

    @Test
    fun `should scrape body content from URL`() {
        val url = "https://techblog.woowahan.com/20939/"

        val content = htmlScraper.scrapeContent(url)

        println("=== Scraped Content (first 500 chars) ===")
        println(content.take(500))
        println("...")
        println("\n=== Total Length: ${content.length} characters ===")

        assertTrue(content.isNotBlank(), "Content should not be blank")
        assertTrue(content.length > 100, "Content should have substantial text")
    }
}

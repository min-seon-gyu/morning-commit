package server.morningcommit

import com.rometools.rome.io.SyndFeedInput
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import server.morningcommit.scraper.HtmlScraper
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class RssParsingTest {

    private val rssHttpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private fun fetchRssFeed(rssUrl: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(rssUrl))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        return rssHttpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    private fun sanitizeXml(xml: String): String {
        return xml
            .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]"), "")
    }

    private fun toLocalDateTime(date: Date?): LocalDateTime? {
        return date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    }

    val base = LocalDateTime.now().minusYears(2)

    /**
     * RSS 주소만 바꿔서 실행하면 피드 항목을 파싱해서 출력합니다.
     */
    @Test
    fun parseRssFeed() {
        val rssUrl = "https://medium.com/feed/musinsa-tech"

        val rawXml = fetchRssFeed(rssUrl)
        val sanitizedXml = sanitizeXml(rawXml)
        val feed = SyndFeedInput().build(StringReader(sanitizedXml))

        println("=== Feed Info ===")
        println("Title       : ${feed.title}")
        println("Description : ${feed.description}")
        println("Link        : ${feed.link}")
        println("Feed Type   : ${feed.feedType}")
        println("Total Items : ${feed.entries.size}")
        println()

        feed.entries.filter { entry ->
            val publishDate = toLocalDateTime(entry.publishedDate ?: entry.updatedDate)
            publishDate != null && publishDate.isAfter(base)
        }.forEachIndexed { index, entry ->
            val publishedDate = entry.publishedDate ?: entry.updatedDate
            val localDate = publishedDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
            val categories = entry.categories.joinToString(", ") { it.name }
            val description = entry.description?.value
                ?.replace(Regex("<[^>]*>"), "")
                ?.take(200)
                ?: "(no description)"

            val contentLength = entry.contents.firstOrNull()?.value?.length ?: 0

            println("--- [$index] ---")
            println("Title         : ${entry.title}")
            println("Link          : ${entry.link}")
            println("Published     : $localDate")
            println("Author        : ${entry.author}")
            println("Categories    : $categories")
            println("Description   : $description")
            println("Content:Encoded : $contentLength chars")
            println()
        }
    }

    /**
     * RSS 피드의 첫 번째 글 본문을 가져옵니다.
     * 1순위: 스크래핑, 2순위: RSS content:encoded, 3순위: description
     */
    @Test
    fun parseRssAndScrapeFirstEntry() {
        val rssUrl = "https://medium.com/feed/musinsa-tech"

        val rawXml = fetchRssFeed(rssUrl)
        val sanitizedXml = sanitizeXml(rawXml)
        val feed = SyndFeedInput().build(StringReader(sanitizedXml))

        val firstEntry = feed.entries.firstOrNull()
        if (firstEntry == null) {
            println("No entries found in feed")
            return
        }

        println("=== First Entry ===")
        println("Title : ${firstEntry.title}")
        println("Link  : ${firstEntry.link}")
        println()

        // 1순위: 스크래핑 시도
        val scraper = HtmlScraper()
        val content = try {
            val scraped = scraper.scrapeContent(firstEntry.link)
            println("[Source: Scraping]")
            scraped
        } catch (e: Exception) {
            println("[Scraping failed: ${e.message}]")

            // 2순위: RSS content:encoded (Medium 등 전체 본문 제공 피드)
            val rssContent = firstEntry.contents.firstOrNull()?.value
            if (!rssContent.isNullOrBlank()) {
                val text = Jsoup.parse(rssContent).text()
                println("[Source: RSS content:encoded]")
                text
            } else {
                // 3순위: RSS description
                val desc = firstEntry.description?.value ?: ""
                val text = Jsoup.parse(desc).text()
                println("[Source: RSS description]")
                text
            }
        }

        println("Content Length : ${content.length} chars")
        println("Content (500자) : ${content.take(500)}")
    }
}

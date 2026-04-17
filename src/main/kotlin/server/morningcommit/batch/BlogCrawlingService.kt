package server.morningcommit.batch

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import server.morningcommit.ai.service.SummaryService
import server.morningcommit.domain.BlogSource
import server.morningcommit.domain.Difficulty
import server.morningcommit.domain.Post
import server.morningcommit.repository.PostRepository
import server.morningcommit.scraper.HtmlScraper
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil

@Service
class BlogCrawlingService(
    private val postRepository: PostRepository,
    private val htmlScraper: HtmlScraper,
    private val summaryService: SummaryService
) {
    private val log = KotlinLogging.logger {}

    private val rssHttpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun processSource(blogSource: BlogSource, cutoff: LocalDateTime): List<Post> {
        log.info { "Processing blog: ${blogSource.blog.displayName}" }

        return try {
            val existingLinks = postRepository.findLinksByBlog(blogSource.blog)
            val entries = fetchRecentEntries(blogSource.rssUrl, cutoff)
            val posts = analyzeEntriesConcurrently(entries, existingLinks, blogSource)

            log.info { "Processed ${posts.size} new posts from ${blogSource.blog.displayName}" }
            posts
        } catch (e: Exception) {
            log.error(e) { "Failed to process blog source: ${blogSource.blog.displayName}" }
            emptyList()
        }
    }

    private fun fetchRecentEntries(rssUrl: String, cutoff: LocalDateTime): List<SyndEntry> {
        val rawXml = fetchRssFeed(rssUrl)
        val sanitizedXml = sanitizeXml(rawXml)
        val feed = SyndFeedInput().build(StringReader(sanitizedXml))

        return feed.entries.filter { entry ->
            val publishDate = toLocalDateTime(entry.publishedDate ?: entry.updatedDate)
            publishDate != null && publishDate.isAfter(cutoff)
        }
    }

    private fun analyzeEntriesConcurrently(
        entries: List<SyndEntry>,
        existingLinks: Set<String>,
        blogSource: BlogSource
    ): List<Post> {
        val semaphore = Semaphore(5)

        return runBlocking(Dispatchers.IO) {
            entries.map { entry ->
                async {
                    try {
                        semaphore.withPermit { analyzeEntry(entry, existingLinks, blogSource) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.error(e) { "Failed to process entry: ${entry.title}" }
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun analyzeEntry(
        entry: SyndEntry,
        existingLinks: Set<String>,
        blogSource: BlogSource
    ): Post? {
        val link = entry.link ?: return null
        if (link in existingLinks) return null

        val fullContent = extractContent(entry, link)
        val analysisResult = summaryService.analyze(fullContent)

        if (analysisResult == null) {
            log.warn { "AI 분석 실패로 포스트 건너뜀: ${entry.title}" }
            return null
        }
        if (analysisResult.isPromotional) return null

        val readingTimeMin = ceil(fullContent.length / 500.0).toInt().coerceAtLeast(1)
        val difficulty = try {
            Difficulty.valueOf(analysisResult.difficulty)
        } catch (e: IllegalArgumentException) {
            Difficulty.INTERMEDIATE
        }

        return Post(
            title = entry.title ?: "Untitled",
            link = link,
            summary = analysisResult.summary,
            keyInsight = analysisResult.keyInsight,
            tags = analysisResult.tags,
            difficulty = difficulty,
            readingTimeMin = readingTimeMin,
            publishDate = toLocalDateTime(entry.publishedDate ?: entry.updatedDate),
            blog = blogSource.blog
        )
    }

    private fun extractContent(entry: SyndEntry, link: String): String {
        return try {
            htmlScraper.scrapeContent(link)
        } catch (e: Exception) {
            val rssContent = entry.contents.firstOrNull()?.value
            if (!rssContent.isNullOrBlank()) {
                Jsoup.parse(rssContent).text()
            } else {
                entry.description?.value ?: ""
            }
        }
    }

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
}

package server.morningcommit.batch

import com.rometools.rome.io.SyndFeedInput
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.morningcommit.ai.service.SummaryService
import server.morningcommit.config.RedisConfig
import server.morningcommit.domain.BlogSource
import server.morningcommit.domain.Difficulty
import server.morningcommit.domain.Post
import server.morningcommit.repository.PostRepository
import server.morningcommit.scraper.HtmlScraper
import server.morningcommit.service.BlogSourceService
import server.morningcommit.service.PostSearchService
import org.jsoup.Jsoup
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.ceil

@Configuration
class BlogCrawlingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val blogSourceService: BlogSourceService,
    private val postRepository: PostRepository,
    private val htmlScraper: HtmlScraper,
    private val summaryService: SummaryService,
    private val cacheManager: CacheManager,
    private val postSearchService: PostSearchService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun blogCrawlingJob(): Job {
        return JobBuilder("blogCrawlingJob", jobRepository)
            .start(crawlingStep())
            .build()
    }

    @Bean
    fun crawlingStep(): Step {
        return StepBuilder("crawlingStep", jobRepository)
            .chunk<BlogSource, List<Post>>(1, transactionManager)
            .reader(blogSourceReader())
            .processor(blogSourceProcessor())
            .writer(postListWriter())
            .build()
    }

    @Bean
    @StepScope
    fun blogSourceReader(): ItemReader<BlogSource> {
        val sources = mutableListOf<BlogSource>()
        var initialized = false

        return ItemReader<BlogSource> {
            if (!initialized) {
                sources.addAll(blogSourceService.findActiveSources())
                initialized = true
            }
            sources.removeFirstOrNull()
        }
    }

    @Bean
    @StepScope
    fun blogSourceProcessor(): ItemProcessor<BlogSource, List<Post>> {
        val base = LocalDateTime.now().minusDays(2)

        return ItemProcessor<BlogSource, List<Post>> { blogSource ->
            log.info("Processing blog: ${blogSource.blog.displayName}")

            try {
                val existingLinks = postRepository.findLinksByBlog(blogSource.blog)
                val rawXml = fetchRssFeed(blogSource.rssUrl)
                val sanitizedXml = sanitizeXml(rawXml)
                val feed = SyndFeedInput().build(StringReader(sanitizedXml))

                feed.entries
                    .filter { entry ->
                        val publishDate = toLocalDateTime(entry.publishedDate ?: entry.updatedDate)
                        publishDate != null && publishDate.isAfter(base)
                    }
                    .mapNotNull { entry ->
                        try {
                            val link = entry.link ?: return@mapNotNull null

                            if (link in existingLinks) {
                                return@mapNotNull null
                            }

                            val fullContent = try {
                                htmlScraper.scrapeContent(link)
                            } catch (e: Exception) {
                                val rssContent = entry.contents.firstOrNull()?.value
                                if (!rssContent.isNullOrBlank()) {
                                    Jsoup.parse(rssContent).text()
                                } else {
                                    entry.description?.value ?: ""
                                }
                            }

                            val analysisResult = summaryService.analyze(fullContent)

                            if (analysisResult == null) {
                                log.warn("AI 분석 실패로 포스트 건너뜀: ${entry.title}")
                                return@mapNotNull null
                            }

                            if (analysisResult.isPromotional) {
                                return@mapNotNull null
                            }

                            val readingTimeMin = ceil(fullContent.length / 500.0).toInt().coerceAtLeast(1)
                            val difficulty = try {
                                Difficulty.valueOf(analysisResult.difficulty)
                            } catch (e: IllegalArgumentException) {
                                Difficulty.INTERMEDIATE
                            }

                            Post(
                                title = entry.title ?: "Untitled", link = link, summary = analysisResult.summary,
                                keyInsight = analysisResult.keyInsight, tags = analysisResult.tags,
                                difficulty = difficulty, readingTimeMin = readingTimeMin,
                                publishDate = toLocalDateTime(entry.publishedDate ?: entry.updatedDate),
                                blog = blogSource.blog
                            )
                        } catch (e: Exception) {
                            log.error("Failed to process entry: ${entry.title}", e)

                            null
                        }
                    }
                    .also { posts ->
                        log.info("Processed ${posts.size} new posts from ${blogSource.blog.displayName}")
                    }
            } catch (e: Exception) {
                log.error("Failed to process blog source: ${blogSource.blog.displayName}", e)
                emptyList()
            }
        }
    }

    @Bean
    fun postListWriter(): ItemWriter<List<Post>> {
        return ItemWriter { chunk: Chunk<out List<Post>> ->
            val allPosts = chunk.items.flatten()
            if (allPosts.isEmpty()) return@ItemWriter

            val existingLinks = postRepository.findExistingLinks(allPosts.map { it.link })
            val newPosts = allPosts.filter { it.link !in existingLinks }

            if (newPosts.isNotEmpty()) {
                val savedPosts = postRepository.saveAll(newPosts)

                cacheManager.getCache(RedisConfig.POST_LISTING)?.clear()

                try {
                    postSearchService.indexPosts(savedPosts.toList())
                } catch (e: Exception) {
                    log.error("Failed to index posts to Elasticsearch", e)
                }

                log.info("Saved ${newPosts.size} posts (${allPosts.size - newPosts.size} duplicates skipped)")
            }
        }
    }

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
}

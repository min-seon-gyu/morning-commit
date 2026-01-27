package server.morningcommit.batch

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import server.morningcommit.ai.service.SummaryService
import server.morningcommit.domain.BlogSource
import server.morningcommit.domain.Post
import server.morningcommit.repository.BlogSourceRepository
import server.morningcommit.repository.PostRepository
import server.morningcommit.scraper.HtmlScraper
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
class BlogCrawlingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val blogSourceRepository: BlogSourceRepository,
    private val postRepository: PostRepository,
    private val htmlScraper: HtmlScraper,
    private val summaryService: SummaryService
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
    fun blogSourceReader(): ItemReader<BlogSource> {
        val sources = mutableListOf<BlogSource>()
        var initialized = false

        return ItemReader<BlogSource> {
            if (!initialized) {
                sources.addAll(blogSourceRepository.findByIsActiveTrue())
                initialized = true
                log.info("Loaded ${sources.size} active blog sources")
            }
            sources.removeFirstOrNull()
        }
    }

    @Bean
    fun blogSourceProcessor(): ItemProcessor<BlogSource, List<Post>> {
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)

        return ItemProcessor<BlogSource, List<Post>> { blogSource ->
            log.info("Processing blog: ${blogSource.blog.displayName}")

            try {
                val feedUrl = URI(blogSource.rssUrl).toURL()
                val feed = XmlReader(feedUrl).use { reader ->
                    SyndFeedInput().build(reader)
                }

                feed.entries
                    .filter { entry ->
                        val publishDate = entry.publishedDate?.toInstant()
                            ?.atZone(ZoneId.systemDefault())
                            ?.toLocalDateTime()
                        publishDate != null && publishDate.isAfter(sevenDaysAgo)
                    }
                    .mapNotNull { entry ->
                        try {
                            val link = entry.link ?: return@mapNotNull null

                            if (postRepository.existsByLink(link)) {
                                log.debug("Skipping existing post: $link")
                                return@mapNotNull null
                            }

                            val fullContent = try {
                                htmlScraper.scrapeContent(link)
                            } catch (e: Exception) {
                                log.warn("Failed to scrape content from $link: ${e.message}")
                                entry.description?.value ?: ""
                            }

                            val summary = summaryService.summarize(fullContent)

                            val publishDate = entry.publishedDate?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDateTime()

                            Post(
                                title = entry.title ?: "Untitled",
                                link = link,
                                description = summary,
                                publishDate = publishDate,
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
            chunk.items.flatten().forEach { post ->
                try {
                    postRepository.save(post)
                    log.info("Saved post: ${post.title}")
                } catch (e: DataIntegrityViolationException) {
                    log.warn("Duplicate post skipped: ${post.link}")
                }
            }
        }
    }
}

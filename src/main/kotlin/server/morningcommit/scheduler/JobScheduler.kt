package server.morningcommit.scheduler

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.ai.service.SummaryService
import server.morningcommit.config.RedisConfig
import server.morningcommit.repository.PostRepository
import server.morningcommit.repository.PostSendHistoryRepository
import server.morningcommit.repository.PostSearchRepository
import server.morningcommit.scraper.HtmlScraper

@Component
class JobScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("blogCrawlingJob") private val blogCrawlingJob: Job,
    @Qualifier("emailDeliveryJob") private val emailDeliveryJob: Job,
    private val postRepository: PostRepository,
    private val postSearchRepository: PostSearchRepository,
    private val postSendHistoryRepository: PostSendHistoryRepository,
    private val htmlScraper: HtmlScraper,
    private val summaryService: SummaryService,
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun initJob() {
        log.info("애플리케이션 시작: 기존 포스트 홍보글 재검사를 실행합니다.")

        val allPosts = postRepository.findAll()
        log.info("총 ${allPosts.size}개 포스트를 재검사합니다.")

        val promotionalPostIds = mutableListOf<Long>()
        val promotionalEsIds = mutableListOf<String>()

        for (post in allPosts) {
            try {
                val content = try {
                    htmlScraper.scrapeContent(post.link)
                } catch (e: Exception) {
                    log.warn("스크래핑 실패 (${post.title}): ${e.message}, summary로 대체 분석")
                    post.summary.joinToString(" ")
                }

                val result = summaryService.analyze(content)

                if (result.isPromotional) {
                    log.info("홍보글 감지 - 삭제 대상: [${post.blog}] ${post.title}")
                    promotionalPostIds.add(post.id!!)
                    promotionalEsIds.add(post.id.toString())
                }
            } catch (e: Exception) {
                log.error("재검사 실패 (${post.title}): ${e.message}", e)
            }
        }

        if (promotionalPostIds.isNotEmpty()) {
            postSendHistoryRepository.deleteAllByPostIdIn(promotionalPostIds)
            postRepository.deleteAllByIdInBatch(promotionalPostIds)

            try {
                postSearchRepository.deleteAllById(promotionalEsIds)
            } catch (e: Exception) {
                log.error("Elasticsearch 삭제 실패: ${e.message}", e)
            }

            cacheManager.getCache(RedisConfig.POST_LISTING)?.clear()
            cacheManager.getCache(RedisConfig.POST_SEARCH)?.clear()

            log.info("홍보글 정리 완료: ${promotionalPostIds.size}개 삭제 (전체 ${allPosts.size}개 중)")
        } else {
            log.info("홍보글 정리 완료: 삭제 대상 없음")
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    fun runCrawlingJob() {
        try {
            val crawlingParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val crawlingExecution = jobLauncher.run(blogCrawlingJob, crawlingParams)

            if (crawlingExecution.status.isUnsuccessful) {
                log.error("blogCrawlingJob failed.")

                return
            }

            log.info("=== Blog Crawling Job Completed ===")
        } catch (e: Exception) {
            log.error("Blog crawling job failed: ${e.message}", e)
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    fun runEmailDeliveryJob() {
        try {
            val deliveryParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val deliveryExecution = jobLauncher.run(emailDeliveryJob, deliveryParams)

            if (deliveryExecution.status.isUnsuccessful) {
                log.error("emailDeliveryJob failed.")

                return
            }

            log.info("=== Email Delivery Job Completed ===")
        } catch (e: Exception) {
            log.error("Email delivery job failed: ${e.message}", e)
        }
    }
}

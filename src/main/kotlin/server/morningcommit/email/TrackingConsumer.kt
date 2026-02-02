package server.morningcommit.email

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.config.RedisConfig
import server.morningcommit.domain.ClickLog
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.repository.ClickLogRepository

@Component
class TrackingConsumer(
    private val clickLogRepository: ClickLogRepository,
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitMqConfig.TRACKING_QUEUE_NAME])
    fun handleClickLogEvent(event: ClickLogEvent) {
        try {
            val clickLog = ClickLog(
                subscriberId = event.subscriberId, targetUrl = event.targetUrl, clickedAt = event.timestamp
            )

            clickLogRepository.save(clickLog)
            cacheManager.getCache(RedisConfig.ANALYTICS_DASHBOARD)?.clear()

            log.info("Successfully saved click log for subscriberId=${event.subscriberId}")
        } catch (e: Exception) {
            log.error("Failed to save click log: ${e.message}", e)

            throw e
        }
    }
}

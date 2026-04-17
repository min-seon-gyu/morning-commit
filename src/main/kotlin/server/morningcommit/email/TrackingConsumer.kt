package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.config.RedisConfig
import server.morningcommit.domain.ClickLog
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.repository.ClickLogRepository
import server.morningcommit.util.runLogging

@Component
class TrackingConsumer(
    private val clickLogRepository: ClickLogRepository,
    private val cacheManager: CacheManager
) {
    private val log = KotlinLogging.logger {}

    @RabbitListener(queues = [RabbitMqConfig.TRACKING_QUEUE_NAME], concurrency = "2-5")
    fun handleClickLogEvent(event: ClickLogEvent) {
        log.runLogging("Failed to save click log for subscriberId=${event.subscriberId}") {
            val clickLog = ClickLog(
                subscriberId = event.subscriberId, targetUrl = event.targetUrl, clickedAt = event.timestamp
            )

            clickLogRepository.save(clickLog)
            cacheManager.getCache(RedisConfig.ANALYTICS_DASHBOARD)?.clear()

            log.info { "Successfully saved click log for subscriberId=${event.subscriberId}" }
        }
    }
}

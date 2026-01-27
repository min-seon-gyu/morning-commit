package server.morningcommit.email

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.domain.ClickLog
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.repository.ClickLogRepository

@Component
class TrackingConsumer(
    private val clickLogRepository: ClickLogRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitMqConfig.TRACKING_QUEUE_NAME])
    fun handleClickLogEvent(event: ClickLogEvent) {
        log.info("Received click log event: subscriberId=${event.subscriberId}, url=${event.targetUrl}")

        try {
            val clickLog = ClickLog(
                subscriberId = event.subscriberId,
                targetUrl = event.targetUrl,
                clickedAt = event.timestamp
            )

            clickLogRepository.save(clickLog)
            log.info("Successfully saved click log for subscriberId=${event.subscriberId}")
        } catch (e: Exception) {
            log.error("Failed to save click log: ${e.message}", e)
            throw e
        }
    }
}

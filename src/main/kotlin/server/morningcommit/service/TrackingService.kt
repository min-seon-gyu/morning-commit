package server.morningcommit.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqProperties
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.exception.InvalidRequestException
import java.time.LocalDateTime

@Service
class TrackingService(
    private val rabbitTemplate: RabbitTemplate,
    private val postLinkValidator: PostLinkValidator,
    private val rabbitMqProperties: RabbitMqProperties
) {

    fun trackClick(url: String, subscriberId: Long): String {
        if (!postLinkValidator.exists(url)) {
            throw InvalidRequestException("유효하지 않은 URL입니다: $url")
        }

        val event = ClickLogEvent(subscriberId = subscriberId, targetUrl = url, timestamp = LocalDateTime.now())
        rabbitTemplate.convertAndSend(
            rabbitMqProperties.tracking.exchange,
            rabbitMqProperties.tracking.routingKey,
            event
        )

        return url
    }
}

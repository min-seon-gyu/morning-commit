package server.morningcommit.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.exception.InvalidRequestException
import server.morningcommit.repository.PostRepository
import java.time.LocalDateTime

@Service
class TrackingService(
    private val rabbitTemplate: RabbitTemplate,
    private val postRepository: PostRepository
) {

    fun trackClick(url: String, subscriberId: Long): String {
        if (!postRepository.existsByLink(url)) {
            throw InvalidRequestException("유효하지 않은 URL입니다: $url")
        }

        val event = ClickLogEvent(subscriberId = subscriberId, targetUrl = url, timestamp = LocalDateTime.now())
        rabbitTemplate.convertAndSend(RabbitMqConfig.TRACKING_EXCHANGE_NAME, RabbitMqConfig.TRACKING_ROUTING_KEY, event)

        return url
    }
}

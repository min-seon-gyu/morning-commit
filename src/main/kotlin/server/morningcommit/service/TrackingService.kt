package server.morningcommit.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.repository.PostRepository
import java.time.LocalDateTime

@Service
class TrackingService(
    private val rabbitTemplate: RabbitTemplate,
    private val postRepository: PostRepository
) {

    sealed interface TrackResult {
        data class Success(val url: String) : TrackResult
        data object InvalidUrl : TrackResult
    }

    fun trackClick(url: String, subscriberId: Long): TrackResult {
        if (!postRepository.existsByLink(url)) {
            return TrackResult.InvalidUrl
        }

        val event = ClickLogEvent(subscriberId = subscriberId, targetUrl = url, timestamp = LocalDateTime.now())
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, RabbitMqConfig.TRACKING_ROUTING_KEY, event)

        return TrackResult.Success(url)
    }
}

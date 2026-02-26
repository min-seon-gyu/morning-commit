package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.EmailRequest

@Service
class EmailProducer(
    private val rabbitTemplate: RabbitTemplate
) {
    private val log = KotlinLogging.logger {}

    fun sendEmailEvent(request: EmailRequest) {
        log.info { "Publishing email event for: ${request.email} with postId: ${request.postId}" }

        rabbitTemplate.convertAndSend(RabbitMqConfig.EMAIL_EXCHANGE_NAME, RabbitMqConfig.EMAIL_ROUTING_KEY, request)
    }
}

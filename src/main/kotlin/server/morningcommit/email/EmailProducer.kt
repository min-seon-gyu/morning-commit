package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqProperties
import server.morningcommit.email.dto.EmailRequest

@Service
class EmailProducer(
    private val rabbitTemplate: RabbitTemplate,
    private val rabbitMqProperties: RabbitMqProperties
) {
    private val log = KotlinLogging.logger {}

    fun sendEmailEvent(request: EmailRequest) {
        log.info { "Publishing email event for: ${request.email} with postId: ${request.postId}" }

        rabbitTemplate.convertAndSend(
            rabbitMqProperties.email.exchange,
            rabbitMqProperties.email.routingKey,
            request
        )
    }
}

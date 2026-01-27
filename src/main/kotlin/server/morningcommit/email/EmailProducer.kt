package server.morningcommit.email

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.EmailRequest

@Service
class EmailProducer(
    private val rabbitTemplate: RabbitTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendEmailEvent(request: EmailRequest) {
        log.info("Publishing email event for: ${request.email} with ${request.postIds.size} posts")
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.EXCHANGE_NAME,
            RabbitMqConfig.ROUTING_KEY,
            request
        )
    }
}

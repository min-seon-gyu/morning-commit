package server.morningcommit.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.rabbitmq")
data class RabbitMqProperties(
    val email: QueueBindings,
    val tracking: QueueBindings
) {
    data class QueueBindings(
        val exchange: String,
        val queue: String,
        val routingKey: String,
        val dlx: String,
        val dlq: String,
        val dlqRoutingKey: String
    )
}

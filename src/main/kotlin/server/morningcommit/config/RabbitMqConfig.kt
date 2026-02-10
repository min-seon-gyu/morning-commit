package server.morningcommit.config

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
class RabbitMqConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val QUEUE_NAME = "email-queue"
        const val EXCHANGE_NAME = "email-exchange"
        const val ROUTING_KEY = "send-email"

        const val TRACKING_QUEUE_NAME = "tracking-queue"
        const val TRACKING_ROUTING_KEY = "tracking-log"

        const val DLX_EXCHANGE_NAME = "email-dlx"
        const val EMAIL_DLQ_NAME = "email-queue-dlq"
        const val TRACKING_DLQ_NAME = "tracking-queue-dlq"
    }

    // ==========================================
    // Main Exchange
    // ==========================================

    @Bean
    fun emailExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    // ==========================================
    // Dead Letter Exchange & Queues
    // ==========================================

    @Bean
    fun deadLetterExchange(): DirectExchange {
        return DirectExchange(DLX_EXCHANGE_NAME)
    }

    @Bean
    fun emailDeadLetterQueue(): Queue {
        return Queue(EMAIL_DLQ_NAME, true)
    }

    @Bean
    fun trackingDeadLetterQueue(): Queue {
        return Queue(TRACKING_DLQ_NAME, true)
    }

    @Bean
    fun emailDlqBinding(emailDeadLetterQueue: Queue, deadLetterExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailDeadLetterQueue)
            .to(deadLetterExchange)
            .with(ROUTING_KEY)
    }

    @Bean
    fun trackingDlqBinding(trackingDeadLetterQueue: Queue, deadLetterExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingDeadLetterQueue)
            .to(deadLetterExchange)
            .with(TRACKING_ROUTING_KEY)
    }

    // ==========================================
    // Main Queues (with DLX argument)
    // ==========================================

    @Bean
    fun emailQueue(): Queue {
        return QueueBuilder.durable(QUEUE_NAME)
            .deadLetterExchange(DLX_EXCHANGE_NAME)
            .build()
    }

    @Bean
    fun emailBinding(emailQueue: Queue, emailExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailQueue)
            .to(emailExchange)
            .with(ROUTING_KEY)
    }

    @Bean
    fun trackingQueue(): Queue {
        return QueueBuilder.durable(TRACKING_QUEUE_NAME)
            .deadLetterExchange(DLX_EXCHANGE_NAME)
            .build()
    }

    @Bean
    fun trackingBinding(trackingQueue: Queue, emailExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingQueue)
            .to(emailExchange)
            .with(TRACKING_ROUTING_KEY)
    }

    // ==========================================
    // Message Converter & RabbitTemplate
    // ==========================================

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun publishRetryTemplate(): RetryTemplate {
        return RetryTemplate().apply {
            setRetryPolicy(SimpleRetryPolicy(3))
            setBackOffPolicy(
                ExponentialBackOffPolicy().apply {
                    initialInterval = 1000L
                    multiplier = 2.0
                    maxInterval = 10000L
                }
            )
        }
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter,
        publishRetryTemplate: RetryTemplate
    ): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply {
            this.messageConverter = messageConverter
            this.setRetryTemplate(publishRetryTemplate)

            setConfirmCallback { correlationData, ack, cause ->
                if (!ack) {
                    log.warn("Message not confirmed by broker. cause={}, correlationData={}", cause, correlationData)
                }
            }

            setReturnsCallback { returned ->
                log.warn(
                    "Message returned from broker. exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.exchange,
                    returned.routingKey,
                    returned.replyCode,
                    returned.replyText
                )
            }
        }
    }
}

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
        const val EMAIL_EXCHANGE_NAME = "email-exchange"
        const val EMAIL_QUEUE_NAME = "email-queue"
        const val EMAIL_ROUTING_KEY = "send-email"
        const val EMAIL_DLX_NAME = "email-queue-dlx"
        const val EMAIL_DLQ_NAME = "email-queue-dlq"
        const val EMAIL_DLQ_ROUTING_KEY = "email-dead-letter"

        const val TRACKING_EXCHANGE_NAME = "tracking-exchange"
        const val TRACKING_QUEUE_NAME = "tracking-queue"
        const val TRACKING_ROUTING_KEY = "tracking-log"
        const val TRACKING_DLX_NAME = "tracking-queue-dlx"
        const val TRACKING_DLQ_NAME = "tracking-queue-dlq"
        const val TRACKING_DLQ_ROUTING_KEY = "tracking-dead-letter"
    }

    // ==========================================
    // Main Exchange
    // ==========================================

    @Bean
    fun emailExchange(): DirectExchange {
        return DirectExchange(EMAIL_EXCHANGE_NAME)
    }

    @Bean
    fun trackingExchange(): DirectExchange {
        return DirectExchange(TRACKING_EXCHANGE_NAME)
    }

    // ==========================================
    // Dead Letter Exchange & Queues
    // ==========================================

    @Bean
    fun emailDlx(): DirectExchange {
        return DirectExchange(EMAIL_DLX_NAME)
    }

    @Bean
    fun trackingDlx(): DirectExchange {
        return DirectExchange(TRACKING_DLX_NAME)
    }

    @Bean
    fun emailDlq(): Queue {
        return Queue(EMAIL_DLQ_NAME, true)
    }

    @Bean
    fun trackingDlq(): Queue {
        return Queue(TRACKING_DLQ_NAME, true)
    }

    @Bean
    fun emailDlqBinding(emailDlq: Queue, emailDlx: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailDlq)
            .to(emailDlx)
            .with(EMAIL_ROUTING_KEY)
    }

    @Bean
    fun trackingDlqBinding(trackingDlq: Queue, trackingDlx: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingDlq)
            .to(trackingDlx)
            .with(TRACKING_ROUTING_KEY)
    }

    // ==========================================
    // Main Queues (with DLX argument)
    // ==========================================

    @Bean
    fun emailQueue(): Queue {
        return QueueBuilder.durable(EMAIL_QUEUE_NAME)
            .deadLetterExchange(EMAIL_DLX_NAME)
            .deadLetterRoutingKey(EMAIL_DLQ_ROUTING_KEY)
            .build()
    }

    @Bean
    fun emailBinding(emailQueue: Queue, emailExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailQueue)
            .to(emailExchange)
            .with(EMAIL_ROUTING_KEY)
    }

    @Bean
    fun trackingQueue(): Queue {
        return QueueBuilder.durable(TRACKING_QUEUE_NAME)
            .deadLetterExchange(TRACKING_DLX_NAME)
            .deadLetterRoutingKey(TRACKING_DLQ_ROUTING_KEY)
            .build()
    }

    @Bean
    fun trackingBinding(trackingQueue: Queue, trackingExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingQueue)
            .to(trackingExchange)
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

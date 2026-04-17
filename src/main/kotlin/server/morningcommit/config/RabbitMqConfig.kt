package server.morningcommit.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableConfigurationProperties(RabbitMqProperties::class)
class RabbitMqConfig(
    private val properties: RabbitMqProperties
) {
    private val log = KotlinLogging.logger {}

    // ==========================================
    // Main Exchange
    // ==========================================

    @Bean
    fun emailExchange(): DirectExchange = DirectExchange(properties.email.exchange)

    @Bean
    fun trackingExchange(): DirectExchange = DirectExchange(properties.tracking.exchange)

    // ==========================================
    // Dead Letter Exchange & Queues
    // ==========================================

    @Bean
    fun emailDlx(): DirectExchange = DirectExchange(properties.email.dlx)

    @Bean
    fun trackingDlx(): DirectExchange = DirectExchange(properties.tracking.dlx)

    @Bean
    fun emailDlq(): Queue = Queue(properties.email.dlq, true)

    @Bean
    fun trackingDlq(): Queue = Queue(properties.tracking.dlq, true)

    @Bean
    fun emailDlqBinding(emailDlq: Queue, emailDlx: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailDlq)
            .to(emailDlx)
            .with(properties.email.routingKey)
    }

    @Bean
    fun trackingDlqBinding(trackingDlq: Queue, trackingDlx: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingDlq)
            .to(trackingDlx)
            .with(properties.tracking.routingKey)
    }

    // ==========================================
    // Main Queues (with DLX argument)
    // ==========================================

    @Bean
    fun emailQueue(): Queue {
        return QueueBuilder.durable(properties.email.queue)
            .deadLetterExchange(properties.email.dlx)
            .deadLetterRoutingKey(properties.email.dlqRoutingKey)
            .build()
    }

    @Bean
    fun emailBinding(emailQueue: Queue, emailExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailQueue)
            .to(emailExchange)
            .with(properties.email.routingKey)
    }

    @Bean
    fun trackingQueue(): Queue {
        return QueueBuilder.durable(properties.tracking.queue)
            .deadLetterExchange(properties.tracking.dlx)
            .deadLetterRoutingKey(properties.tracking.dlqRoutingKey)
            .build()
    }

    @Bean
    fun trackingBinding(trackingQueue: Queue, trackingExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(trackingQueue)
            .to(trackingExchange)
            .with(properties.tracking.routingKey)
    }

    // ==========================================
    // Message Converter & RabbitTemplate
    // ==========================================

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()

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
                    log.warn { "Message not confirmed by broker. cause=$cause, correlationData=$correlationData" }
                }
            }

            setReturnsCallback { returned ->
                log.warn { "Message returned from broker. exchange=${returned.exchange}, routingKey=${returned.routingKey}, replyCode=${returned.replyCode}, replyText=${returned.replyText}" }
            }
        }
    }
}

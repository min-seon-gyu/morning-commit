package server.morningcommit.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    companion object {
        const val QUEUE_NAME = "email-queue"
        const val EXCHANGE_NAME = "email-exchange"
        const val ROUTING_KEY = "send-email"
    }

    @Bean
    fun emailQueue(): Queue {
        return Queue(QUEUE_NAME, true)
    }

    @Bean
    fun emailExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    @Bean
    fun emailBinding(emailQueue: Queue, emailExchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(emailQueue)
            .to(emailExchange)
            .with(ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply {
            this.messageConverter = messageConverter
        }
    }
}

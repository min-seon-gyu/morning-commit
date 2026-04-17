package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.repository.PostRepository
import server.morningcommit.util.runLogging

@Component
class EmailConsumer(
    private val postRepository: PostRepository,
    private val emailService: EmailService
) {
    private val log = KotlinLogging.logger {}

    @RabbitListener(queues = [RabbitMqConfig.EMAIL_QUEUE_NAME], concurrency = "3-10")
    fun handleEmailRequest(request: EmailRequest) {
        log.info { "Received email request for: ${request.email}" }

        log.runLogging("Failed to process email request for ${request.email}") {
            val post = postRepository.findById(request.postId).orElseThrow {
                IllegalStateException("Post not found for ID: ${request.postId}")
            }

            emailService.sendNewsletter(request.email, post, request.subscriberId)
        }
    }
}

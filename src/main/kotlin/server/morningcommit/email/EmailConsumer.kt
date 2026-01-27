package server.morningcommit.email

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.repository.PostRepository

@Component
class EmailConsumer(
    private val postRepository: PostRepository,
    private val emailService: EmailService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitMqConfig.QUEUE_NAME])
    fun handleEmailRequest(request: EmailRequest) {
        log.info("Received email request for: ${request.email}")

        try {
            val posts = postRepository.findAllById(request.postIds)

            if (posts.isEmpty()) {
                log.warn("No posts found for IDs: ${request.postIds}")
                return
            }

            emailService.sendNewsletter(request.email, posts)
            log.info("Successfully processed email request for: ${request.email}")
        } catch (e: Exception) {
            log.error("Failed to process email request for ${request.email}: ${e.message}", e)
            throw e
        }
    }
}

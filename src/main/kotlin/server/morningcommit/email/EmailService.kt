package server.morningcommit.email

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import server.morningcommit.domain.Post
import server.morningcommit.email.dto.TrackedPost
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${app.base-url}")
    private val baseUrl: String,
    @Value("\${spring.mail.username}")
    private val from: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendNewsletter(to: String, posts: List<Post>, subscriberId: Long) {
        try {
            val trackedPosts = posts.map { post -> toTrackedPost(post, subscriberId) }
            val htmlContent = renderTemplate(trackedPosts, to)
            val message: MimeMessage = mailSender.createMimeMessage()

            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(from)
                setTo(to)
                setSubject("[MorningCommit] 오늘의 기술 블로그 다이제스트")
                setText(htmlContent, true)
            }

            mailSender.send(message)
            log.info("Newsletter sent to: $to")
        } catch (e: Exception) {
            log.error("Failed to send newsletter to $to: ${e.message}", e)
            throw e
        }
    }

    private fun toTrackedPost(post: Post, subscriberId: Long): TrackedPost {
        val encodedUrl = URLEncoder.encode(post.link, StandardCharsets.UTF_8)
        val trackedLink = "$baseUrl/track?url=$encodedUrl&subscriberId=$subscriberId"

        return TrackedPost(
            title = post.title,
            link = trackedLink,
            description = post.description,
            publishDate = post.publishDate,
            blog = post.blog
        )
    }

    private fun renderTemplate(posts: List<TrackedPost>, subscriberEmail: String): String {
        val context = Context().apply {
            setVariable("posts", posts)
            setVariable("subscriberEmail", subscriberEmail)
            setVariable("baseUrl", baseUrl)
        }
        return templateEngine.process("newsletter", context)
    }
}

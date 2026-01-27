package server.morningcommit.email

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import server.morningcommit.domain.Post

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendNewsletter(to: String, posts: List<Post>) {
        try {
            val htmlContent = renderTemplate(posts)
            val message: MimeMessage = mailSender.createMimeMessage()

            MimeMessageHelper(message, true, "UTF-8").apply {
                setTo(to)
                setSubject("[MorningCommit] Today's Tech Blog Digest")
                setText(htmlContent, true)
            }

            mailSender.send(message)
            log.info("Newsletter sent to: $to")
        } catch (e: Exception) {
            log.error("Failed to send newsletter to $to: ${e.message}", e)
            throw e
        }
    }

    private fun renderTemplate(posts: List<Post>): String {
        val context = Context().apply {
            setVariable("posts", posts)
        }
        return templateEngine.process("newsletter", context)
    }
}

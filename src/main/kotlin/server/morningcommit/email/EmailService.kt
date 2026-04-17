package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.morningcommit.domain.Post
import server.morningcommit.email.dto.TrackedPost
import server.morningcommit.util.runLogging

@Service
class EmailService(
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailSender: EmailSender,
    private val emailUrlGenerator: EmailUrlGenerator
) {
    private val log = KotlinLogging.logger {}

    fun sendVerificationEmail(to: String, code: String) {
        log.runLogging("Failed to send verification email to $to") {
            val htmlContent = emailTemplateRenderer.renderVerificationTemplate(code)

            emailSender.sendHtmlEmail(to = to, subject = EmailSubject.VERIFICATION, htmlContent = htmlContent)
        }
    }

    fun sendNewsletter(to: String, post: Post, subscriberId: Long) {
        log.runLogging("Failed to send newsletter to $to") {
            val trackedPost = toTrackedPost(post, subscriberId)
            val htmlContent = emailTemplateRenderer.renderNewsletterTemplate(trackedPost, to)

            emailSender.sendHtmlEmail(to = to, subject = EmailSubject.NEWSLETTER, htmlContent = htmlContent)
        }
    }

    private fun toTrackedPost(post: Post, subscriberId: Long): TrackedPost {
        val trackedLink = emailUrlGenerator.generateTrackingUrl(originalUrl = post.link, subscriberId = subscriberId)

        return TrackedPost(
            title = post.title, link = trackedLink, summary = post.summary, keyInsight = post.keyInsight,
            publishDate = post.publishDate, blog = post.blog
        )
    }
}

package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.morningcommit.domain.Post
import server.morningcommit.email.dto.TrackedPost

@Service
class EmailService(
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailSender: EmailSender,
    private val emailUrlGenerator: EmailUrlGenerator
) {
    private val log = KotlinLogging.logger {}

    fun sendVerificationEmail(to: String, code: String) {
        try {
            val htmlContent = emailTemplateRenderer.renderVerificationTemplate(code)

            emailSender.sendHtmlEmail(to = to, subject = "[MorningCommit] 이메일 인증번호", htmlContent = htmlContent)
        } catch (e: Exception) {
            log.error(e) { "Failed to send verification email to $to: ${e.message}" }
            throw e
        }
    }

    fun sendNewsletter(to: String, post: Post, subscriberId: Long) {
        try {
            val trackedPost = toTrackedPost(post, subscriberId)
            val htmlContent = emailTemplateRenderer.renderNewsletterTemplate(trackedPost, to)

            emailSender.sendHtmlEmail(to = to, subject = "[MorningCommit] 오늘의 기술 블로그 다이제스트", htmlContent = htmlContent)
        } catch (e: Exception) {
            log.error(e) { "Failed to send newsletter to $to: ${e.message}" }
            throw e
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

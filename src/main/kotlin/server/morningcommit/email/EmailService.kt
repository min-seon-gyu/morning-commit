package server.morningcommit.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import server.morningcommit.domain.Post
import server.morningcommit.email.dto.TrackedPost

@Service
class EmailService(
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailSender: EmailSender,
    private val emailUrlGenerator: EmailUrlGenerator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerificationEmail(to: String, code: String) {
        try {
            val htmlContent = emailTemplateRenderer.renderVerificationTemplate(code)

            emailSender.sendHtmlEmail(to = to, subject = "[MorningCommit] 이메일 인증번호", htmlContent = htmlContent)
        } catch (e: Exception) {
            log.error("Failed to send verification email to $to: ${e.message}", e)
            throw e
        }
    }

    fun sendNewsletter(to: String, post: Post, subscriberId: Long) {
        try {
            val trackedPost = toTrackedPost(post, subscriberId)
            val htmlContent = emailTemplateRenderer.renderNewsletterTemplate(trackedPost, to)

            emailSender.sendHtmlEmail(to = to, subject = "[MorningCommit] 오늘의 기술 블로그 다이제스트", htmlContent = htmlContent)
        } catch (e: Exception) {
            log.error("Failed to send newsletter to $to: ${e.message}", e)
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

package server.morningcommit.email

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}")
    private val fromEmail: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        try {
            val message: MimeMessage = mailSender.createMimeMessage()

            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(fromEmail)
                setTo(to)
                setSubject(subject)
                setText(htmlContent, true)
            }

            mailSender.send(message)
            log.info("Email sent successfully to: $to")
        } catch (e: Exception) {
            log.error("Failed to send email to $to: ${e.message}", e)
            throw e
        }
    }
}

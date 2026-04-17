package server.morningcommit.email

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import server.morningcommit.util.runLogging

@Component
class EmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}")
    private val fromEmail: String
) {
    private val log = KotlinLogging.logger {}

    fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        log.runLogging("Failed to send email to $to") {
            val message: MimeMessage = mailSender.createMimeMessage()

            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(fromEmail)
                setTo(to)
                setSubject(subject)
                setText(htmlContent, true)
            }

            mailSender.send(message)
            log.info { "Email sent successfully to: $to" }
        }
    }
}

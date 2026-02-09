package server.morningcommit.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import server.morningcommit.email.dto.TrackedPost

@Component
class EmailTemplateRenderer(
    private val templateEngine: TemplateEngine,
    @Value("\${app.base-url}")
    private val baseUrl: String
) {

    fun renderNewsletterTemplate(post: TrackedPost, subscriberEmail: String): String {
        val context = Context().apply {
            setVariable("post", post)
            setVariable("subscriberEmail", subscriberEmail)
            setVariable("baseUrl", baseUrl)
        }

        return templateEngine.process("newsletter", context)
    }

    fun renderVerificationTemplate(code: String): String {
        val context = Context().apply {
            setVariable("code", code)
            setVariable("baseUrl", baseUrl)
        }

        return templateEngine.process("verification", context)
    }
}

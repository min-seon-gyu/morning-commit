package server.morningcommit.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class EmailUrlGenerator(
    @Value("\${app.base-url}") private val baseUrl: String
) {
    fun generateTrackingUrl(originalUrl: String, subscriberId: Long): String {
        val encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8)

        return "$baseUrl/track?url=$encodedUrl&subscriberId=$subscriberId"
    }
}

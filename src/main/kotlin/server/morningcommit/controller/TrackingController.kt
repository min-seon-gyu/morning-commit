package server.morningcommit.controller

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.ClickLogEvent
import java.time.LocalDateTime

@RestController
class TrackingController(
    private val rabbitTemplate: RabbitTemplate
) {

    @GetMapping("/track")
    fun track(
        @RequestParam url: String,
        @RequestParam subscriberId: Long
    ): RedirectView {
        val event = ClickLogEvent(
            subscriberId = subscriberId,
            targetUrl = url,
            timestamp = LocalDateTime.now()
        )

        rabbitTemplate.convertAndSend(
            RabbitMqConfig.EXCHANGE_NAME,
            RabbitMqConfig.TRACKING_ROUTING_KEY,
            event
        )

        return RedirectView(url).apply {
            setStatusCode(org.springframework.http.HttpStatus.FOUND)
        }
    }
}

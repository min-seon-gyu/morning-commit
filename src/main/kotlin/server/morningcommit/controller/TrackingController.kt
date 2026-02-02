package server.morningcommit.controller

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import server.morningcommit.config.RabbitMqConfig
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.repository.PostRepository
import java.net.URI
import java.time.LocalDateTime

@RestController
class TrackingController(
    private val rabbitTemplate: RabbitTemplate,
    private val postRepository: PostRepository
) {

    @GetMapping("/track")
    fun track(@RequestParam url: String, @RequestParam subscriberId: Long): ResponseEntity<Void> {
        if (!postRepository.existsByLink(url)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }

        val event = ClickLogEvent(subscriberId = subscriberId, targetUrl = url, timestamp = LocalDateTime.now())
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, RabbitMqConfig.TRACKING_ROUTING_KEY, event)

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(url))
            .build()
    }
}

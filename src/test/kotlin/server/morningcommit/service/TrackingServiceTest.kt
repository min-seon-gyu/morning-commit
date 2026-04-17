package server.morningcommit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.amqp.rabbit.core.RabbitTemplate
import server.morningcommit.config.RabbitMqProperties
import server.morningcommit.email.dto.ClickLogEvent
import server.morningcommit.exception.InvalidRequestException

@ExtendWith(MockKExtension::class)
class TrackingServiceTest {

    @MockK
    private lateinit var rabbitTemplate: RabbitTemplate

    @MockK
    private lateinit var postLinkValidator: PostLinkValidator

    @MockK
    private lateinit var rabbitMqProperties: RabbitMqProperties

    @InjectMockKs
    private lateinit var trackingService: TrackingService

    private val bindings = RabbitMqProperties.QueueBindings(
        exchange = "tracking-exchange", queue = "tracking-queue", routingKey = "tracking-log",
        dlx = "tracking-queue-dlx", dlq = "tracking-queue-dlq", dlqRoutingKey = "tracking-dead-letter"
    )

    @Test
    fun `유효한 URL이면 이벤트를 발행하고 원본 URL을 반환한다`() {
        every { postLinkValidator.exists("https://ok.com/1") } returns true
        every { rabbitMqProperties.tracking } returns bindings
        every {
            rabbitTemplate.convertAndSend("tracking-exchange", "tracking-log", any<ClickLogEvent>())
        } just Runs

        val result = trackingService.trackClick("https://ok.com/1", 42L)

        assertEquals("https://ok.com/1", result)
        verify { rabbitTemplate.convertAndSend("tracking-exchange", "tracking-log", any<ClickLogEvent>()) }
    }

    @Test
    fun `유효하지 않은 URL이면 InvalidRequestException을 던지고 이벤트를 발행하지 않는다`() {
        every { postLinkValidator.exists("https://evil.com") } returns false

        val ex = assertThrows(InvalidRequestException::class.java) {
            trackingService.trackClick("https://evil.com", 1L)
        }

        assertEquals("유효하지 않은 URL입니다: https://evil.com", ex.message)
        verify(exactly = 0) { rabbitTemplate.convertAndSend(any<String>(), any<String>(), any() as Any) }
    }
}

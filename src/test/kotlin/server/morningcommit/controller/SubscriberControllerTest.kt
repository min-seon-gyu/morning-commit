package server.morningcommit.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import server.morningcommit.controller.dto.SendVerificationRequest
import server.morningcommit.controller.dto.UnsubscribeRequest
import server.morningcommit.controller.dto.VerifyRequest
import server.morningcommit.email.EmailService
import server.morningcommit.exception.DuplicateException
import server.morningcommit.exception.VerificationFailedException
import server.morningcommit.service.SubscriberService
import server.morningcommit.service.UnsubscribeTokenService

@WebMvcTest(SubscriberController::class)
class SubscriberControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var subscriberService: SubscriberService

    @MockkBean
    private lateinit var emailService: EmailService

    @MockkBean
    private lateinit var unsubscribeTokenService: UnsubscribeTokenService

    @Test
    fun `POST send-verification - 신규 이메일이면 200 반환하고 인증 이메일 발송`() {
        every { subscriberService.isAlreadyActive("new@example.com") } returns false
        every { subscriberService.generateAndSave("new@example.com") } returns "123456"
        every { emailService.sendVerificationEmail("new@example.com", "123456") } just Runs

        mockMvc.perform(
            post("/api/subscribers/send-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendVerificationRequest("new@example.com")))
        ).andExpect(status().isOk)

        verify { emailService.sendVerificationEmail("new@example.com", "123456") }
    }

    @Test
    fun `POST send-verification - 이미 활성 구독자면 S002 반환`() {
        every { subscriberService.isAlreadyActive("active@example.com") } returns true

        mockMvc.perform(
            post("/api/subscribers/send-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendVerificationRequest("active@example.com")))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("S002"))
    }

    @Test
    fun `POST send-verification - 잘못된 이메일 형식이면 C001 반환`() {
        mockMvc.perform(
            post("/api/subscribers/send-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendVerificationRequest("not-an-email")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    @Test
    fun `POST send-verification - 본문이 비어있으면 C001 반환`() {
        mockMvc.perform(
            post("/api/subscribers/send-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    @Test
    fun `POST verify - 올바른 코드면 201 Created 반환`() {
        every { subscriberService.verifyAndSubscribe("u@example.com", "123456") } just Runs

        mockMvc.perform(
            post("/api/subscribers/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VerifyRequest("u@example.com", "123456")))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `POST verify - 인증 실패 시 S003 반환`() {
        every { subscriberService.verifyAndSubscribe(any(), any()) } throws
            VerificationFailedException("인증 코드가 일치하지 않습니다")

        mockMvc.perform(
            post("/api/subscribers/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VerifyRequest("u@example.com", "000000")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("S003"))
    }

    @Test
    fun `POST verify - 코드가 6자리 숫자 아니면 C001 반환`() {
        mockMvc.perform(
            post("/api/subscribers/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VerifyRequest("u@example.com", "abc")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    @Test
    fun `POST unsubscribe - 유효한 토큰이면 200 반환`() {
        every { unsubscribeTokenService.validateToken("u@example.com", "valid-token") } returns true
        every { subscriberService.unsubscribe("u@example.com") } just Runs

        mockMvc.perform(
            post("/api/subscribers/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UnsubscribeRequest("u@example.com", "valid-token")))
        ).andExpect(status().isOk)

        verify { subscriberService.unsubscribe("u@example.com") }
    }

    @Test
    fun `POST unsubscribe - 유효하지 않은 토큰이면 C001 반환`() {
        every { unsubscribeTokenService.validateToken(any(), any()) } returns false

        mockMvc.perform(
            post("/api/subscribers/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UnsubscribeRequest("u@example.com", "bad-token")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    private fun handleDuplicate() {
        every { subscriberService.isAlreadyActive(any()) } throws DuplicateException("dup")
    }
}

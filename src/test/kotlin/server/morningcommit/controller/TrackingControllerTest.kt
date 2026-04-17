package server.morningcommit.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import server.morningcommit.exception.InvalidRequestException
import server.morningcommit.service.TrackingService

@WebMvcTest(TrackingController::class)
class TrackingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var trackingService: TrackingService

    @Test
    fun `GET track - 유효한 URL이면 302로 원본 URL 리다이렉트`() {
        every { trackingService.trackClick("https://example.com/post/1", 42L) } returns "https://example.com/post/1"

        mockMvc.perform(get("/track").param("url", "https://example.com/post/1").param("subscriberId", "42"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "https://example.com/post/1"))
    }

    @Test
    fun `GET track - 유효하지 않은 URL이면 C001 반환`() {
        every { trackingService.trackClick(any(), any()) } throws InvalidRequestException("유효하지 않은 URL입니다")

        mockMvc.perform(get("/track").param("url", "https://bad.com").param("subscriberId", "42"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    @Test
    fun `GET track - 필수 파라미터 누락 시 C001 반환`() {
        mockMvc.perform(get("/track").param("url", "https://example.com"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }

    @Test
    fun `GET track - subscriberId가 숫자 아니면 C001 반환`() {
        mockMvc.perform(get("/track").param("url", "https://example.com").param("subscriberId", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("C001"))
    }
}

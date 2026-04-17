package server.morningcommit.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import server.morningcommit.config.RestPage
import server.morningcommit.domain.Blog
import server.morningcommit.domain.Post
import server.morningcommit.service.AnalyticsService
import server.morningcommit.service.AnalyticsService.AnalyticsDashboard
import server.morningcommit.service.AnalyticsService.AnalyticsResult
import server.morningcommit.service.PostService
import server.morningcommit.service.UnsubscribeTokenService

@WebMvcTest(ViewController::class)
class ViewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var postService: PostService

    @MockkBean
    private lateinit var analyticsService: AnalyticsService

    @MockkBean
    private lateinit var unsubscribeTokenService: UnsubscribeTokenService

    private fun emptyPage() = RestPage<Post>(emptyList(), 0, 9, 0)

    @Test
    fun `GET index - 블로그 필터 없이 호출하면 findAll 호출`() {
        every { postService.findAll(any()) } returns emptyPage()

        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("posts", "blogs"))
    }

    @Test
    fun `GET index - 블로그 필터가 있으면 findByBlog 호출`() {
        every { postService.findByBlog(Blog.TOSS_TECH, any()) } returns emptyPage()

        mockMvc.perform(get("/").param("blog", "TOSS_TECH"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attribute("currentBlog", Blog.TOSS_TECH))
    }

    @Test
    fun `GET analytics - 데이터 있으면 dashboard 속성 설정`() {
        val dashboard = AnalyticsDashboard(
            totalClicks = 100, uniqueClickers = 10, totalSubscribers = 5,
            topPosts = emptyList(), blogClicks = emptyList(), dailyTrend = emptyList(),
            maxPostClicks = 1, maxBlogClicks = 1, maxDailyClicks = 1
        )
        every { analyticsService.getDashboard() } returns AnalyticsResult.Success(dashboard)

        mockMvc.perform(get("/analytics"))
            .andExpect(status().isOk)
            .andExpect(view().name("analytics"))
            .andExpect(model().attribute("dashboard", dashboard))
    }

    @Test
    fun `GET analytics - 데이터 없으면 noData 속성 설정`() {
        every { analyticsService.getDashboard() } returns AnalyticsResult.NoData

        mockMvc.perform(get("/analytics"))
            .andExpect(status().isOk)
            .andExpect(view().name("analytics"))
            .andExpect(model().attribute("noData", true))
    }

    @Test
    fun `GET unsubscribe - 유효한 토큰이면 confirm 상태로 렌더`() {
        every { unsubscribeTokenService.validateToken("u@example.com", "ok") } returns true

        mockMvc.perform(get("/unsubscribe").param("email", "u@example.com").param("token", "ok"))
            .andExpect(status().isOk)
            .andExpect(view().name("unsubscribe"))
            .andExpect(model().attribute("state", "confirm"))
    }

    @Test
    fun `GET unsubscribe - 유효하지 않은 토큰이면 error 상태로 렌더`() {
        every { unsubscribeTokenService.validateToken(any(), any()) } returns false

        mockMvc.perform(get("/unsubscribe").param("email", "u@example.com").param("token", "bad"))
            .andExpect(status().isOk)
            .andExpect(view().name("unsubscribe"))
            .andExpect(model().attribute("state", "error"))
    }

}

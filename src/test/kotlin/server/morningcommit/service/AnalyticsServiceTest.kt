package server.morningcommit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import server.morningcommit.domain.Blog
import server.morningcommit.repository.ClickLogRepository
import server.morningcommit.repository.SubscriberRepository
import server.morningcommit.service.dto.BlogClickCount
import server.morningcommit.service.dto.DailyClickCount
import server.morningcommit.service.dto.PostClickCount
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class AnalyticsServiceTest {

    @MockK
    private lateinit var clickLogRepository: ClickLogRepository

    @MockK
    private lateinit var subscriberRepository: SubscriberRepository

    @InjectMockKs
    private lateinit var analyticsService: AnalyticsService

    @Nested
    @DisplayName("getDashboard")
    inner class GetDashboard {

        @Test
        fun `클릭 로그가 없으면 NoData를 반환한다`() {
            every { clickLogRepository.countTotalClicks() } returns 0L

            val result = analyticsService.getDashboard()

            assertEquals(AnalyticsService.AnalyticsResult.NoData, result)
            verify(exactly = 0) { clickLogRepository.findClickCountsByPost() }
            verify(exactly = 0) { subscriberRepository.countByIsActiveTrue() }
        }

        @Test
        fun `클릭이 있으면 Success를 반환하고 모든 집계 값을 담는다`() {
            val topPosts = listOf(
                PostClickCount(1, "포스트 A", Blog.KAKAO_TECH, "https://a.com", 50L),
                PostClickCount(2, "포스트 B", Blog.TOSS_TECH, "https://b.com", 30L)
            )
            val blogClicks = listOf(
                BlogClickCount(Blog.KAKAO_TECH, 100L),
                BlogClickCount(Blog.TOSS_TECH, 50L)
            )
            val dailyTrend = listOf(
                DailyClickCount(LocalDate.of(2026, 4, 1), 20L),
                DailyClickCount(LocalDate.of(2026, 4, 2), 35L)
            )
            every { clickLogRepository.countTotalClicks() } returns 150L
            every { clickLogRepository.findClickCountsByPost() } returns topPosts
            every { clickLogRepository.findClickCountsByBlog() } returns blogClicks
            every { clickLogRepository.findDailyClickCounts(any<LocalDateTime>()) } returns dailyTrend
            every { clickLogRepository.countUniqueClickers() } returns 42L
            every { subscriberRepository.countByIsActiveTrue() } returns 10L

            val result = analyticsService.getDashboard()

            val success = assertInstanceOf(AnalyticsService.AnalyticsResult.Success::class.java, result)
            val dashboard = success.data
            assertEquals(150L, dashboard.totalClicks)
            assertEquals(42L, dashboard.uniqueClickers)
            assertEquals(10L, dashboard.totalSubscribers)
            assertEquals(2, dashboard.topPosts.size)
            assertEquals(50L, dashboard.maxPostClicks)
            assertEquals(100L, dashboard.maxBlogClicks)
            assertEquals(35L, dashboard.maxDailyClicks)
        }

        @Test
        fun `topPosts는 최대 10개까지만 담는다`() {
            val fifteenPosts = (1..15).map {
                PostClickCount(it.toLong(), "포스트 $it", Blog.KAKAO_TECH, "https://e.com/$it", (20 - it).toLong())
            }
            every { clickLogRepository.countTotalClicks() } returns 200L
            every { clickLogRepository.findClickCountsByPost() } returns fifteenPosts
            every { clickLogRepository.findClickCountsByBlog() } returns emptyList()
            every { clickLogRepository.findDailyClickCounts(any<LocalDateTime>()) } returns emptyList()
            every { clickLogRepository.countUniqueClickers() } returns 5L
            every { subscriberRepository.countByIsActiveTrue() } returns 3L

            val result = analyticsService.getDashboard()

            val success = assertInstanceOf(AnalyticsService.AnalyticsResult.Success::class.java, result)
            assertEquals(10, success.data.topPosts.size)
        }

        @Test
        fun `max 값 계산 시 빈 리스트는 기본값 1을 반환한다`() {
            every { clickLogRepository.countTotalClicks() } returns 5L
            every { clickLogRepository.findClickCountsByPost() } returns emptyList()
            every { clickLogRepository.findClickCountsByBlog() } returns emptyList()
            every { clickLogRepository.findDailyClickCounts(any<LocalDateTime>()) } returns emptyList()
            every { clickLogRepository.countUniqueClickers() } returns 1L
            every { subscriberRepository.countByIsActiveTrue() } returns 0L

            val result = analyticsService.getDashboard()

            val success = assertInstanceOf(AnalyticsService.AnalyticsResult.Success::class.java, result)
            assertEquals(1L, success.data.maxPostClicks)
            assertEquals(1L, success.data.maxBlogClicks)
            assertEquals(1L, success.data.maxDailyClicks)
            assertTrue(success.data.topPosts.isEmpty())
        }

        @Test
        fun `일일 트렌드 조회 시 최근 30일 이전 시점을 기준으로 조회한다`() {
            val since = slotOfSince()
            every { clickLogRepository.countTotalClicks() } returns 1L
            every { clickLogRepository.findClickCountsByPost() } returns emptyList()
            every { clickLogRepository.findClickCountsByBlog() } returns emptyList()
            every { clickLogRepository.findDailyClickCounts(capture(since)) } returns emptyList()
            every { clickLogRepository.countUniqueClickers() } returns 1L
            every { subscriberRepository.countByIsActiveTrue() } returns 0L

            analyticsService.getDashboard()

            val capturedSince = since.captured
            val now = LocalDateTime.now()
            val expected = now.minusDays(30)
            val diffMinutes = java.time.Duration.between(capturedSince, expected).abs().toMinutes()
            assertTrue(diffMinutes <= 1, "조회 기준 시점은 30일 전이어야 함 (오차 1분 이내, diff=${diffMinutes}분)")
        }

        private fun slotOfSince() = io.mockk.slot<LocalDateTime>()
    }
}

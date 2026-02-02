package server.morningcommit.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.repository.ClickLogRepository
import server.morningcommit.service.dto.BlogClickCount
import server.morningcommit.service.dto.DailyClickCount
import server.morningcommit.service.dto.PostClickCount
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AnalyticsService(
    private val clickLogRepository: ClickLogRepository
) {

    sealed interface AnalyticsResult {
        data class Success(val data: AnalyticsDashboard) : AnalyticsResult
        data object NoData : AnalyticsResult
    }

    data class AnalyticsDashboard(
        val totalClicks: Long,
        val uniqueClickers: Long,
        val topPosts: List<PostClickCount>,
        val blogClicks: List<BlogClickCount>,
        val dailyTrend: List<DailyClickCount>,
        val maxPostClicks: Long,
        val maxBlogClicks: Long,
        val maxDailyClicks: Long
    )

    fun getDashboard(): AnalyticsResult {
        val totalClicks = clickLogRepository.countTotalClicks()
        if (totalClicks == 0L) {
            return AnalyticsResult.NoData
        }

        val topPosts = clickLogRepository.findClickCountsByPost().take(10)
        val blogClicks = clickLogRepository.findClickCountsByBlog()
        val dailyTrend = clickLogRepository.findDailyClickCounts(
            LocalDateTime.now().minusDays(30)
        )
        val uniqueClickers = clickLogRepository.countUniqueClickers()

        return AnalyticsResult.Success(
            AnalyticsDashboard(
                totalClicks = totalClicks,
                uniqueClickers = uniqueClickers,
                topPosts = topPosts,
                blogClicks = blogClicks,
                dailyTrend = dailyTrend,
                maxPostClicks = topPosts.maxOfOrNull { it.clickCount } ?: 1L,
                maxBlogClicks = blogClicks.maxOfOrNull { it.clickCount } ?: 1L,
                maxDailyClicks = dailyTrend.maxOfOrNull { it.clickCount } ?: 1L
            )
        )
    }
}

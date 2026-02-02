package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import server.morningcommit.domain.ClickLog
import server.morningcommit.service.dto.BlogClickCount
import server.morningcommit.service.dto.DailyClickCount
import server.morningcommit.service.dto.PostClickCount
import java.time.LocalDateTime

interface ClickLogRepository : JpaRepository<ClickLog, Long> {

    @Query(
        """
        SELECT new server.morningcommit.service.dto.PostClickCount(
            p.id, p.title, p.blog, p.link, COUNT(c)
        )
        FROM ClickLog c JOIN Post p ON c.targetUrl = p.link
        GROUP BY p.id, p.title, p.blog, p.link
        ORDER BY COUNT(c) DESC
        """
    )
    fun findClickCountsByPost(): List<PostClickCount>

    @Query(
        """
        SELECT new server.morningcommit.service.dto.BlogClickCount(
            p.blog, COUNT(c)
        )
        FROM ClickLog c JOIN Post p ON c.targetUrl = p.link
        GROUP BY p.blog
        ORDER BY COUNT(c) DESC
        """
    )
    fun findClickCountsByBlog(): List<BlogClickCount>

    @Query(
        """
        SELECT new server.morningcommit.service.dto.DailyClickCount(
            CAST(c.clickedAt AS LocalDate), COUNT(c)
        )
        FROM ClickLog c
        WHERE c.clickedAt >= :since
        GROUP BY CAST(c.clickedAt AS LocalDate)
        ORDER BY CAST(c.clickedAt AS LocalDate) ASC
        """
    )
    fun findDailyClickCounts(@Param("since") since: LocalDateTime): List<DailyClickCount>

    @Query("SELECT COUNT(c) FROM ClickLog c")
    fun countTotalClicks(): Long

    @Query("SELECT COUNT(DISTINCT c.subscriberId) FROM ClickLog c")
    fun countUniqueClickers(): Long
}

package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import server.morningcommit.domain.Post
import java.time.LocalDateTime

interface PostRepository : JpaRepository<Post, Long> {
    fun existsByLink(link: String): Boolean

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :startOfDay")
    fun findTodayPosts(startOfDay: LocalDateTime): List<Post>
}

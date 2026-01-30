package server.morningcommit.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import server.morningcommit.domain.Blog
import server.morningcommit.domain.Post

interface PostRepository : JpaRepository<Post, Long> {
    fun existsByLink(link: String): Boolean

    fun findByBlog(blog: Blog, pageable: Pageable): Page<Post>

    @Query("SELECT p.id FROM Post p")
    fun findAllIds(): List<Long>
}

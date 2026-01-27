package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.Post

interface PostRepository : JpaRepository<Post, Long> {
    fun existsByLink(link: String): Boolean
}

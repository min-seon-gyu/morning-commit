package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.BlogSource

interface BlogSourceRepository : JpaRepository<BlogSource, Long> {
    fun findByIsActiveTrue(): List<BlogSource>
}

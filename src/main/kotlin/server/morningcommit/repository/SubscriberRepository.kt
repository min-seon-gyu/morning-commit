package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.Subscriber

interface SubscriberRepository : JpaRepository<Subscriber, Long> {
    fun findByIsActiveTrue(): List<Subscriber>
    fun existsByEmail(email: String): Boolean
}

package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.Subscriber

interface SubscriberRepository : JpaRepository<Subscriber, Long> {
    fun findByEmail(email: String): Subscriber?
}

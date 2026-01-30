package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import server.morningcommit.domain.PostSendHistory

interface PostSendHistoryRepository : JpaRepository<PostSendHistory, Long> {
    @Modifying
    @Query("DELETE FROM PostSendHistory h WHERE h.subscriberId = :subscriberId")
    fun deleteBySubscriberId(subscriberId: Long)
}

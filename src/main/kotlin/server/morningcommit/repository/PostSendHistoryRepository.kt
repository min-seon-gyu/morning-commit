package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import server.morningcommit.domain.PostSendHistory

interface PostSendHistoryRepository : JpaRepository<PostSendHistory, Long> {

    @Query("SELECT h.postId FROM PostSendHistory h WHERE h.userId = :userId")
    fun findSentPostIdsByUserId(userId: Long): List<Long>

    @Modifying
    @Query("DELETE FROM PostSendHistory h WHERE h.userId = :userId")
    fun deleteByUserId(userId: Long)
}

package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import server.morningcommit.domain.PostSendHistory

interface PostSendHistoryRepository : JpaRepository<PostSendHistory, Long> {

    @Query("SELECT h.postId FROM PostSendHistory h WHERE h.subscriber.id = :subscriberId")
    fun findPostIdsBySubscriberId(subscriberId: Long): Set<Long>

    @Modifying
    @Query("DELETE FROM PostSendHistory h WHERE h.subscriber.id = :subscriberId")
    fun deleteAllBySubscriberId(subscriberId: Long)

    @Modifying
    @Query("DELETE FROM PostSendHistory h WHERE h.postId IN :postIds")
    fun deleteAllByPostIdIn(postIds: Collection<Long>)
}

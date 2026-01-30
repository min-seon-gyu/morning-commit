package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.PostSendHistory

interface PostSendHistoryRepository : JpaRepository<PostSendHistory, Long>

package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "post_send_history",
    indexes = [
        Index(name = "idx_post_send_history_user", columnList = "userId"),
        Index(name = "idx_post_send_history_user_post", columnList = "userId, postId", unique = true)
    ]
)
class PostSendHistory(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val postId: Long,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    var subscriber: Subscriber? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

package server.morningcommit.domain

import jakarta.persistence.*

@Entity
@Table(
    name = "post_send_history",
    indexes = [
        Index(name = "idx_post_send_history_user", columnList = "subscriber_id"),
        Index(name = "idx_post_send_history_user_post", columnList = "subscriber_id, postId", unique = true)
    ]
)
class PostSendHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    val subscriber: Subscriber,

    @Column(nullable = false)
    val postId: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : BaseEntity()

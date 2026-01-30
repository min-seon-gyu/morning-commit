package server.morningcommit.domain

import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "subscriber")
@EntityListeners(AuditingEntityListener::class)
class Subscriber(
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "subscriber", cascade = [CascadeType.ALL], orphanRemoval = true)
    val sendHistories: MutableList<PostSendHistory> = mutableListOf(),

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : BaseEntity()

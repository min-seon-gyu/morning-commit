package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "post",
    indexes = [Index(name = "idx_post_link", columnList = "link", unique = true)]
)
class Post(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, unique = true, length = 700)
    var link: String,

    @Lob
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    var publishDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var blog: Blog,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : BaseEntity()

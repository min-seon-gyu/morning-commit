package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blog_source")
class BlogSource(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var blog: Blog,

    @Column(nullable = false)
    var rssUrl: String,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

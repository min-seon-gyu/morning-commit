package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blog_source")
class BlogSource(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var rssUrl: String,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

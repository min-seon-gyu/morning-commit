package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import server.morningcommit.config.StringListConverter
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
    @Convert(converter = StringListConverter::class)
    var summary: List<String> = emptyList(),

    @Column(length = 500)
    var keyInsight: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var tags: List<String> = emptyList(),

    @Column(length = 20)
    var difficulty: String = "INTERMEDIATE",

    var readingTimeMin: Int = 0,

    var publishDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var blog: Blog,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : BaseEntity()

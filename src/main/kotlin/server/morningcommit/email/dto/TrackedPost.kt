package server.morningcommit.email.dto

import server.morningcommit.domain.Blog
import java.time.LocalDateTime

data class TrackedPost(
    val title: String,
    val link: String,
    val description: String?,
    val publishDate: LocalDateTime?,
    val blog: Blog
)

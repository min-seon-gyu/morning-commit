package server.morningcommit.email.dto

import server.morningcommit.domain.Blog
import java.time.LocalDateTime

data class TrackedPost(
    val title: String,
    val link: String,
    val summary: List<String>,
    val keyInsight: String?,
    val publishDate: LocalDateTime?,
    val blog: Blog
)

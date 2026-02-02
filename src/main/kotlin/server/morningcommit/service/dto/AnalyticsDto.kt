package server.morningcommit.service.dto

import server.morningcommit.domain.Blog
import java.time.LocalDate

data class PostClickCount(
    val postId: Long,
    val title: String,
    val blog: Blog,
    val link: String,
    val clickCount: Long
)

data class BlogClickCount(
    val blog: Blog,
    val clickCount: Long
)

data class DailyClickCount(
    val date: LocalDate,
    val clickCount: Long
)

package server.morningcommit.email.dto

import java.time.LocalDateTime

data class ClickLogEvent(
    val subscriberId: Long,
    val targetUrl: String,
    val timestamp: LocalDateTime
)

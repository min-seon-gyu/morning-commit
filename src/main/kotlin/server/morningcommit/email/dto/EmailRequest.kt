package server.morningcommit.email.dto

data class EmailRequest(
    val subscriberId: Long,
    val email: String,
    val postId: Long
)

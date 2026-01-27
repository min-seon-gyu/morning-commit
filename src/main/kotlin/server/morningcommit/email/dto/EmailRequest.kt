package server.morningcommit.email.dto

data class EmailRequest(
    val email: String,
    val postIds: List<Long>
)

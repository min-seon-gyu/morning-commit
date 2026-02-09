package server.morningcommit.controller.dto

data class UnsubscribeRequest(
    val email: String,
    val token: String
)

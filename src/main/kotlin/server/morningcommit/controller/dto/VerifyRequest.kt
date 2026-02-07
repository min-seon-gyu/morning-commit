package server.morningcommit.controller.dto

data class VerifyRequest(
    val email: String,
    val code: String
)

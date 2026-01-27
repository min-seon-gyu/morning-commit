package server.morningcommit.ai.dto

data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

package server.morningcommit.ai.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import server.morningcommit.ai.client.OpenAiClient
import server.morningcommit.ai.dto.ChatCompletionRequest
import server.morningcommit.ai.dto.Message

@Service
class SummaryService(
    private val openAiClient: OpenAiClient,
    @Value("\${openai.api-key:}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_PROMPT =
            "Summarize the following technical article in 3 bullet points in Korean."
    }

    fun summarize(content: String): String {
        if (apiKey.isBlank()) {
            log.warn("OpenAI API key is not configured")
            return "[요약 불가] API 키가 설정되지 않았습니다."
        }

        return try {
            val request = ChatCompletionRequest(
                messages = listOf(
                    Message(role = "system", content = SYSTEM_PROMPT),
                    Message(role = "user", content = content)
                )
            )

            val response = openAiClient.createChatCompletion(
                authorization = "Bearer $apiKey", request = request
            )

            response.choices.firstOrNull()?.message?.content
                ?: "[요약 불가] 응답이 비어있습니다."
        } catch (e: Exception) {
            log.error("Failed to summarize content: ${e.message}", e)
            "[요약 실패] ${e.message}"
        }
    }
}

package server.morningcommit.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import server.morningcommit.ai.client.OpenAiClient
import server.morningcommit.ai.dto.ChatCompletionRequest
import server.morningcommit.ai.dto.Message
import server.morningcommit.ai.service.dto.BlogAnalysisResult

@Service
class SummaryService(
    private val openAiClient: OpenAiClient,
    private val objectMapper: ObjectMapper,
    @Value("\${openai.api-key:}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_PROMPT = """
            You are a Senior Technical Editor. Analyze the technical blog post provided by the user.
            
            Output MUST be a valid JSON object with the following fields:
            1. "summary": A list of 3 strings. Summarize key points in Korean. Remove polite endings like '합니다'.
            2. "key_insight": A single Korean sentence explaining WHY a developer should read this. (Focus on the problem solved or value gained).
            3. "tags": A list of technical keywords (e.g., "Kotlin", "MSA", "Redis"). Max 5 tags.
            4. "difficulty": Choose one of ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"].
            
            Example format:
            {
              "summary": ["포인트1", "포인트2", "포인트3"],
              "key_insight": "이 글은 ~를 해결하는 방법을 제시합니다.",
              "tags": ["Java", "Spring"],
              "difficulty": "INTERMEDIATE"
            }
            
            DO NOT output markdown code blocks (```json). Just return the raw JSON string.
        """
    }

    fun analyze(content: String): BlogAnalysisResult {
        if (apiKey.isBlank()) {
            log.warn("OpenAI API key is not configured")
            return BlogAnalysisResult(summary = listOf("API 키 설정 필요"))
        }

        return try {
            val request = ChatCompletionRequest(
                messages = listOf(
                    Message(role = "system", content = SYSTEM_PROMPT),
                    Message(role = "user", content = content)
                ),
            )

            val response = openAiClient.createChatCompletion(
                authorization = "Bearer $apiKey", request = request
            )

            val jsonContent = response.choices.firstOrNull()?.message?.content
                ?: return BlogAnalysisResult(summary = listOf("응답 없음"))

            objectMapper.readValue<BlogAnalysisResult>(jsonContent)
        } catch (e: Exception) {
            log.error("Failed to analyze content: ${e.message}", e)

            BlogAnalysisResult(summary = listOf("분석 실패: ${e.message}"))
        }
    }
}

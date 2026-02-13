package server.morningcommit.ai.service

import org.springframework.ai.chat.client.ChatClient.Builder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import server.morningcommit.ai.service.dto.BlogAnalysisResult

@Service
class SummaryService(
    @Value("classpath:/prompts/summary-prompt.st")
    private val promptResource: Resource,
    chatClientBuilder: Builder
) {
    private val chatClient = chatClientBuilder
        .defaultSystem(promptResource)
        .build()

    fun analyze(content: String): BlogAnalysisResult {
        return try {
            chatClient.prompt()
                .user(content)
                .call()
                .entity(BlogAnalysisResult::class.java)
                ?: return BlogAnalysisResult(summary = listOf("응답 없음"))
        } catch (e: Exception) {
            BlogAnalysisResult(summary = listOf("분석 실패: ${e.message}"))
        }
    }
}

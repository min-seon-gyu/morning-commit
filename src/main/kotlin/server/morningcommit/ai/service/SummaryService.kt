package server.morningcommit.ai.service

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)
    private val chatClient = chatClientBuilder
        .defaultSystem(promptResource)
        .build()

    fun analyze(content: String): BlogAnalysisResult? {
        return try {
            chatClient.prompt()
                .user(content)
                .call()
                .entity(BlogAnalysisResult::class.java)
        } catch (e: Exception) {
            log.warn("AI 분석 실패: ${e.message}", e)
            null
        }
    }
}

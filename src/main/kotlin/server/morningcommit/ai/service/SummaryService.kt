package server.morningcommit.ai.service

import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val log = KotlinLogging.logger {}
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
            log.warn(e) { "AI 분석 실패: ${e.message}" }
            null
        }
    }
}

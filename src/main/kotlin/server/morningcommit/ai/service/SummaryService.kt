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
            You are a Senior Technical Editor with STRICT content filtering standards. Analyze the technical blog post provided by the user.

            Output MUST be a valid JSON object with the following fields:
            1. "summary": A list of 3 strings. Summarize key points in Korean. Each point MUST end in noun form (명사형 종결). Example endings: ~방법, ~구현, ~개선, ~활용, ~도입, ~처리, ~분석. Do NOT use verb endings like ~한다, ~이다, ~됨.
            2. "key_insight": A single Korean sentence explaining WHY a developer should read this. MUST end in formal polite form (합니다체/습니다체). Example endings: ~할 수 있습니다, ~를 제시합니다, ~에 도움이 됩니다. Do NOT use plain form like ~있다, ~된다.
            3. "tags": A list of technical keywords (e.g., "Kotlin", "MSA", "Redis"). Max 5 tags.
            4. "difficulty": Choose one of ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]. Do NOT default to INTERMEDIATE — evaluate carefully.
               BEGINNER: introductory tutorials, getting started guides, basic concepts explained for newcomers.
               INTERMEDIATE: practical how-to articles, common patterns, standard library/framework usage.
               ADVANCED: deep dives into internals, performance optimization, complex architecture, distributed systems.
               EXPERT: cutting-edge research, low-level systems (kernel, compiler, JVM internals), novel algorithms, large-scale infrastructure design.
            5. "is_promotional": true if the article does NOT teach reusable technical knowledge. Apply STRICT filtering — when in doubt, mark as promotional.

               === MUST FILTER (is_promotional = true) ===
               [Recruitment & HR]
               - Job postings, recruitment announcements, hiring process explanations
               - Intern/newcomer recruitment, career fair announcements
               - "We're hiring", "Join our team" focused articles

               [Events & Competitions]
               - Hackathon, ideathon, makeathon announcements or recaps
               - Conference, meetup, seminar, workshop announcements or recaps
               - Tech competition results or participation stories
               - Awards, certifications, rankings announcements

               [Company Culture & People]
               - Welcome kit unboxings, office tour, workspace introductions
               - Onboarding process, new employee orientation descriptions
               - Employee interviews, developer stories, "a day in the life"
               - Team introductions, organizational structure descriptions
               - Company culture, work-life balance, benefits descriptions
               - Year-in-review, company milestones, anniversary posts
               - CSR activities, donations, social contribution posts

               [Product & Business]
               - Product launch announcements, feature updates, release notes
               - Service introductions, product recommendations, shopping guides
               - Business results, KPI achievements, growth metrics
               - Partnership announcements, MOU signings
               - Marketing campaigns, promotions, discount events

               [Non-Technical Content]
               - Project retrospectives focused on teamwork/process (not technology)
               - Research/model showcases WITHOUT implementation details or reproducible methodology
               - UX/design case studies without technical implementation
               - Lifestyle, hobbies, book reviews, personal essays
               - Newsletter roundups, link collections without original analysis
               - Surveys, polls, community announcements
               - Open source project announcements without technical deep-dive

               === KEEP (is_promotional = false) ===
               ONLY keep articles that satisfy ALL of these:
               - The MAIN PURPOSE is teaching reusable technical knowledge
               - Contains specific technology, architecture, algorithm, or engineering practice
               - A developer can learn and apply something concrete from the article
               - Has substantial technical depth (code examples, architecture diagrams, performance benchmarks, implementation details)

               Note: A standard "we are hiring" footer common in Korean tech blogs does NOT make an article promotional — ignore footers.
               Note: If an article is 70% promotional and 30% technical, it IS promotional.

            Example format:
            {
              "summary": ["Redis 캐시를 활용한 API 응답 속도 개선", "분산 환경에서의 캐시 무효화 전략 구현", "Spring Boot와 Redis 연동 설정 방법"],
              "key_insight": "대규모 트래픽 환경에서 Redis 캐시를 효과적으로 활용하여 API 성능을 극적으로 개선할 수 있습니다.",
              "tags": ["Redis", "Spring Boot", "Cache"],
              "difficulty": "INTERMEDIATE",
              "is_promotional": false
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

package server.morningcommit.ai.service.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class BlogAnalysisResult(
    val summary: List<String> = emptyList(),
    @JsonProperty("key_insight")
    val keyInsight: String = "",
    val tags: List<String> = emptyList(),
    val difficulty: String = "INTERMEDIATE",
    @JsonProperty("is_promotional")
    val isPromotional: Boolean = false
)

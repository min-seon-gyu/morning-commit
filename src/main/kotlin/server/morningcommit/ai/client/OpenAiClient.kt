package server.morningcommit.ai.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import server.morningcommit.ai.dto.ChatCompletionRequest
import server.morningcommit.ai.dto.ChatCompletionResponse

@FeignClient(name = "openAiClient", url = "https://api.openai.com")
interface OpenAiClient {

    @PostMapping("/v1/chat/completions")
    fun createChatCompletion(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: ChatCompletionRequest
    ): ChatCompletionResponse
}

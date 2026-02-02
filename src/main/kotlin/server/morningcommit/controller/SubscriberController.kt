package server.morningcommit.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.morningcommit.controller.dto.SubscribeRequest
import server.morningcommit.service.SubscriberService
import server.morningcommit.service.SubscriberService.SubscribeResult
import server.morningcommit.service.SubscriberService.UnsubscribeResult

@RestController
@RequestMapping("/api/subscribers")
class SubscriberController(
    private val subscriberService: SubscriberService
) {

    @PostMapping
    fun subscribe(@RequestBody request: SubscribeRequest): ResponseEntity<Map<String, String>> {
        return when (subscriberService.subscribe(request.email)) {
            SubscribeResult.Created -> ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("message" to "구독이 완료되었습니다."))
            SubscribeResult.Reactivated -> ResponseEntity.ok(mapOf("message" to "구독이 다시 활성화되었습니다."))
            SubscribeResult.AlreadyActive -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "이미 구독 중인 이메일입니다."))
        }
    }

    @DeleteMapping
    fun unsubscribe(@RequestParam email: String): ResponseEntity<Map<String, String>> {
        return toUnsubscribeResponse(subscriberService.unsubscribe(email))
    }

    @GetMapping("/unsubscribe")
    fun unsubscribeViaEmail(@RequestParam email: String): ResponseEntity<Map<String, String>> {
        return toUnsubscribeResponse(subscriberService.unsubscribe(email))
    }

    private fun toUnsubscribeResponse(result: UnsubscribeResult): ResponseEntity<Map<String, String>> {
        return when (result) {
            UnsubscribeResult.Success -> ResponseEntity.ok(mapOf("message" to "구독이 취소되었습니다."))
            UnsubscribeResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "해당 이메일의 구독자를 찾을 수 없습니다."))
        }
    }
}

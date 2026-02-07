package server.morningcommit.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.morningcommit.controller.dto.SendVerificationRequest
import server.morningcommit.controller.dto.VerifyRequest
import server.morningcommit.email.EmailService
import server.morningcommit.service.SubscriberService
import server.morningcommit.service.SubscriberService.UnsubscribeResult

@RestController
@RequestMapping("/api/subscribers")
class SubscriberController(
    private val subscriberService: SubscriberService,
    private val emailService: EmailService
) {

    @PostMapping("/send-verification")
    fun sendVerification(@RequestBody request: SendVerificationRequest): ResponseEntity<Map<String, String>> {
        if (subscriberService.isAlreadyActive(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "이미 구독 중인 이메일입니다."))
        }

        val code = subscriberService.generateAndSave(request.email)
        emailService.sendVerificationEmail(request.email, code)

        return ResponseEntity.ok(mapOf("message" to "인증번호가 발송되었습니다."))
    }

    @PostMapping("/verify")
    fun verify(@RequestBody request: VerifyRequest): ResponseEntity<Map<String, String>> {
        return if (subscriberService.verifyAndSubscribe(request.email, request.code)) {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("message" to "구독이 완료되었습니다."))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "인증번호가 유효하지 않습니다."))
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

package server.morningcommit.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.morningcommit.controller.dto.SendVerificationRequest
import server.morningcommit.controller.dto.UnsubscribeRequest
import server.morningcommit.controller.dto.VerifyRequest
import server.morningcommit.email.EmailService
import server.morningcommit.exception.DuplicateException
import server.morningcommit.exception.InvalidRequestException
import server.morningcommit.service.SubscriberService
import server.morningcommit.service.UnsubscribeTokenService

@RestController
@RequestMapping("/api/subscribers")
class SubscriberController(
    private val subscriberService: SubscriberService,
    private val emailService: EmailService,
    private val unsubscribeTokenService: UnsubscribeTokenService
) {

    @PostMapping("/send-verification")
    fun sendVerification(@Valid @RequestBody request: SendVerificationRequest): ResponseEntity<Void> {
        if (subscriberService.isAlreadyActive(request.email)) {
            throw DuplicateException("이미 구독 중인 이메일입니다: ${request.email}")
        }

        val code = subscriberService.generateAndSave(request.email)
        emailService.sendVerificationEmail(request.email, code)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: VerifyRequest): ResponseEntity<Void> {
        subscriberService.verifyAndSubscribe(request.email, request.code)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/unsubscribe")
    fun unsubscribeWithToken(@Valid @RequestBody request: UnsubscribeRequest): ResponseEntity<Void> {
        if (!unsubscribeTokenService.validateToken(request.email, request.token)) {
            throw InvalidRequestException("유효하지 않은 구독 해지 토큰입니다")
        }

        subscriberService.unsubscribe(request.email)

        return ResponseEntity.ok().build()
    }
}

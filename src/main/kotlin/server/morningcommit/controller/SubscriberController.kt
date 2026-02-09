package server.morningcommit.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.morningcommit.controller.dto.SendVerificationRequest
import server.morningcommit.controller.dto.UnsubscribeRequest
import server.morningcommit.controller.dto.VerifyRequest
import server.morningcommit.email.EmailService
import server.morningcommit.service.SubscriberService
import server.morningcommit.service.SubscriberService.UnsubscribeResult
import server.morningcommit.service.UnsubscribeTokenService

@RestController
@RequestMapping("/api/subscribers")
class SubscriberController(
    private val subscriberService: SubscriberService,
    private val emailService: EmailService,
    private val unsubscribeTokenService: UnsubscribeTokenService
) {

    @PostMapping("/send-verification")
    fun sendVerification(@RequestBody request: SendVerificationRequest): ResponseEntity<Void> {
        if (subscriberService.isAlreadyActive(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        val code = subscriberService.generateAndSave(request.email)
        emailService.sendVerificationEmail(request.email, code)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/verify")
    fun verify(@RequestBody request: VerifyRequest): ResponseEntity<Void> {
        return if (subscriberService.verifyAndSubscribe(request.email, request.code)) {
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("/unsubscribe")
    fun unsubscribeWithToken(@RequestBody request: UnsubscribeRequest): ResponseEntity<Void> {
        if (!unsubscribeTokenService.validateToken(request.email, request.token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }

        return when (subscriberService.unsubscribe(request.email)) {
            UnsubscribeResult.Success -> ResponseEntity.ok().build()
            UnsubscribeResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}

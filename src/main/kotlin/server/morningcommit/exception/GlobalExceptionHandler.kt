package server.morningcommit.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse(code = e.errorCode.code, message = e.errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Unexpected error occurred" }
        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.status)
            .body(ErrorResponse(code = ErrorCode.INTERNAL_ERROR.code, message = ErrorCode.INTERNAL_ERROR.message))
    }
}

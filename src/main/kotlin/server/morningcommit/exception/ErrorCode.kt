package server.morningcommit.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 구독 관련 에러
    SUBSCRIBER_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "구독자를 찾을 수 없습니다"),
    DUPLICATE_SUBSCRIBER(HttpStatus.CONFLICT, "S002", "이미 구독 중인 이메일입니다"),
    VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "S003", "인증에 실패했습니다"),

    // 공통 에러
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다")
}

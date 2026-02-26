package server.morningcommit.exception

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)

class NotFoundException(message: String) : BusinessException(ErrorCode.SUBSCRIBER_NOT_FOUND, message)

class DuplicateException(message: String) : BusinessException(ErrorCode.DUPLICATE_SUBSCRIBER, message)

class InvalidRequestException(message: String) : BusinessException(ErrorCode.INVALID_REQUEST, message)

class VerificationFailedException(message: String) : BusinessException(ErrorCode.VERIFICATION_FAILED, message)

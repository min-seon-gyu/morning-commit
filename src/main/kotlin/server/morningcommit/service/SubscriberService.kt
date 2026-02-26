package server.morningcommit.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.domain.Subscriber
import server.morningcommit.exception.NotFoundException
import server.morningcommit.exception.VerificationFailedException
import server.morningcommit.repository.SubscriberRepository
import java.time.Duration
import kotlin.random.Random

@Service
class SubscriberService(
    private val subscriberRepository: SubscriberRepository,
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val KEY_PREFIX = "verification:"
        private const val ATTEMPT_PREFIX = "verification:attempts:"
        private const val MAX_ATTEMPTS = 5
        private val TTL = Duration.ofMinutes(5)
    }

    @Transactional
    fun unsubscribe(email: String) {
        val subscriber = subscriberRepository.findByEmail(email)
            ?: throw NotFoundException("구독자를 찾을 수 없습니다: $email")

        subscriber.isActive = false
    }

    fun isAlreadyActive(email: String): Boolean {
        val subscriber = subscriberRepository.findByEmail(email)

        return subscriber != null && subscriber.isActive
    }

    fun generateAndSave(email: String): String {
        val code = String.format("%06d", Random.nextInt(1_000_000))
        redisTemplate.opsForValue().set("$KEY_PREFIX$email", code, TTL)

        return code
    }

    @Transactional
    fun verifyAndSubscribe(email: String, code: String) {
        val savedCode = redisTemplate.opsForValue().get("$KEY_PREFIX$email")
            ?: throw VerificationFailedException("인증 코드가 만료되었거나 존재하지 않습니다")

        val attemptKey = "$ATTEMPT_PREFIX$email"
        val attempts = redisTemplate.opsForValue().get(attemptKey)?.toIntOrNull() ?: 0

        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete("$KEY_PREFIX$email")
            redisTemplate.delete(attemptKey)

            throw VerificationFailedException("인증 시도 횟수를 초과했습니다")
        }

        if (savedCode != code) {
            redisTemplate.opsForValue().set(attemptKey, (attempts + 1).toString(), TTL)

            throw VerificationFailedException("인증 코드가 일치하지 않습니다")
        }

        redisTemplate.delete("$KEY_PREFIX$email")
        redisTemplate.delete(attemptKey)

        val subscriber = subscriberRepository.findByEmail(email)

        subscriber?.apply { isActive = true } ?: subscriberRepository.save(Subscriber(email = email))
    }
}

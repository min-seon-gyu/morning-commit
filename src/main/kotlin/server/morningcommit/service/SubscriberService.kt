package server.morningcommit.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.domain.Subscriber
import server.morningcommit.repository.SubscriberRepository
import java.time.Duration
import kotlin.random.Random

@Service
class SubscriberService(
    private val subscriberRepository: SubscriberRepository,
    private val redisTemplate: StringRedisTemplate
) {

    sealed interface UnsubscribeResult {
        data object Success : UnsubscribeResult
        data object NotFound : UnsubscribeResult
    }

    companion object {
        private const val KEY_PREFIX = "verification:"
        private val TTL = Duration.ofMinutes(5)
    }

    @Transactional
    fun unsubscribe(email: String): UnsubscribeResult {
        val subscriber = subscriberRepository.findByEmail(email)
            ?: return UnsubscribeResult.NotFound

        subscriber.isActive = false
        return UnsubscribeResult.Success
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

    fun verifyAndSubscribe(email: String, code: String): Boolean {
        val savedCode = redisTemplate.opsForValue().get("$KEY_PREFIX$email")
            ?: return false

        if (savedCode != code) {
            return false
        }

        redisTemplate.delete("$KEY_PREFIX$email")

        subscriberRepository.save(Subscriber(email = email))

        return true
    }
}

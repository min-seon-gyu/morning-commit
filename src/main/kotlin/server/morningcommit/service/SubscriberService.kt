package server.morningcommit.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.domain.Subscriber
import server.morningcommit.repository.SubscriberRepository

@Service
class SubscriberService(
    private val subscriberRepository: SubscriberRepository
) {

    sealed interface SubscribeResult {
        data object Created : SubscribeResult
        data object Reactivated : SubscribeResult
        data object AlreadyActive : SubscribeResult
    }

    sealed interface UnsubscribeResult {
        data object Success : UnsubscribeResult
        data object NotFound : UnsubscribeResult
    }

    @Transactional
    fun subscribe(email: String): SubscribeResult {
        val existing = subscriberRepository.findByEmail(email)

        if (existing != null) {
            if (existing.isActive) {
                return SubscribeResult.AlreadyActive
            }
            existing.isActive = true
            return SubscribeResult.Reactivated
        }

        subscriberRepository.save(Subscriber(email = email))
        return SubscribeResult.Created
    }

    @Transactional
    fun unsubscribe(email: String): UnsubscribeResult {
        val subscriber = subscriberRepository.findByEmail(email)
            ?: return UnsubscribeResult.NotFound

        subscriber.isActive = false
        return UnsubscribeResult.Success
    }
}

package server.morningcommit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import server.morningcommit.domain.Subscriber
import server.morningcommit.exception.NotFoundException
import server.morningcommit.exception.VerificationFailedException
import server.morningcommit.repository.SubscriberRepository
import java.time.Duration

@ExtendWith(MockKExtension::class)
class SubscriberServiceTest {

    @MockK
    private lateinit var subscriberRepository: SubscriberRepository

    @MockK
    private lateinit var redisTemplate: StringRedisTemplate

    @MockK
    private lateinit var valueOperations: ValueOperations<String, String>

    @InjectMockKs
    private lateinit var subscriberService: SubscriberService

    private val email = "test@example.com"
    private val verificationKey = "verification:$email"
    private val attemptKey = "verification:attempts:$email"

    @Nested
    @DisplayName("generateAndSave")
    inner class GenerateAndSave {

        @Test
        fun `6자리 숫자 코드를 생성해 Redis에 TTL 5분으로 저장한다`() {
            every { redisTemplate.opsForValue() } returns valueOperations
            val ttlSlot = slot<Duration>()
            every { valueOperations.set(verificationKey, any(), capture(ttlSlot)) } just Runs

            val code = subscriberService.generateAndSave(email)

            assertTrue(code.matches(Regex("\\d{6}")), "6자리 숫자여야 함: $code")
            assertEquals(Duration.ofMinutes(5), ttlSlot.captured)
            verify(exactly = 1) { valueOperations.set(verificationKey, code, Duration.ofMinutes(5)) }
        }
    }

    @Nested
    @DisplayName("verifyAndSubscribe")
    inner class VerifyAndSubscribe {

        @Test
        fun `올바른 코드 입력 시 신규 구독자를 생성하고 인증 키를 삭제한다`() {
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(verificationKey) } returns "123456"
            every { valueOperations.get(attemptKey) } returns null
            every { redisTemplate.delete(verificationKey) } returns true
            every { redisTemplate.delete(attemptKey) } returns true
            every { subscriberRepository.findByEmail(email) } returns null
            every { subscriberRepository.save(any<Subscriber>()) } answers { firstArg() }

            subscriberService.verifyAndSubscribe(email, "123456")

            verify { redisTemplate.delete(verificationKey) }
            verify { redisTemplate.delete(attemptKey) }
            verify { subscriberRepository.save(match<Subscriber> { it.email == email && it.isActive }) }
        }

        @Test
        fun `기존 비활성 구독자는 재활성화된다`() {
            val existing = Subscriber(email = email, isActive = false)
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(verificationKey) } returns "123456"
            every { valueOperations.get(attemptKey) } returns null
            every { redisTemplate.delete(any<String>()) } returns true
            every { subscriberRepository.findByEmail(email) } returns existing

            subscriberService.verifyAndSubscribe(email, "123456")

            assertTrue(existing.isActive, "기존 구독자가 활성화되어야 함")
            verify(exactly = 0) { subscriberRepository.save(any<Subscriber>()) }
        }

        @Test
        fun `인증 코드가 Redis에 없으면 VerificationFailedException을 던진다`() {
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(verificationKey) } returns null

            val ex = assertThrows(VerificationFailedException::class.java) {
                subscriberService.verifyAndSubscribe(email, "123456")
            }
            assertEquals("인증 코드가 만료되었거나 존재하지 않습니다", ex.message)
        }

        @Test
        fun `코드가 일치하지 않으면 시도 횟수를 증가시키고 예외를 던진다`() {
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(verificationKey) } returns "123456"
            every { valueOperations.get(attemptKey) } returns "2"
            val attemptSlot = slot<String>()
            every { valueOperations.set(attemptKey, capture(attemptSlot), Duration.ofMinutes(5)) } just Runs

            val ex = assertThrows(VerificationFailedException::class.java) {
                subscriberService.verifyAndSubscribe(email, "999999")
            }

            assertEquals("인증 코드가 일치하지 않습니다", ex.message)
            assertEquals("3", attemptSlot.captured)
        }

        @Test
        fun `시도 횟수가 최대치에 도달하면 모든 키를 삭제하고 예외를 던진다`() {
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(verificationKey) } returns "123456"
            every { valueOperations.get(attemptKey) } returns "5"
            every { redisTemplate.delete(verificationKey) } returns true
            every { redisTemplate.delete(attemptKey) } returns true

            val ex = assertThrows(VerificationFailedException::class.java) {
                subscriberService.verifyAndSubscribe(email, "123456")
            }

            assertEquals("인증 시도 횟수를 초과했습니다", ex.message)
            verify { redisTemplate.delete(verificationKey) }
            verify { redisTemplate.delete(attemptKey) }
        }
    }

    @Nested
    @DisplayName("unsubscribe")
    inner class Unsubscribe {

        @Test
        fun `존재하는 구독자의 isActive를 false로 변경한다`() {
            val subscriber = Subscriber(email = email, isActive = true)
            every { subscriberRepository.findByEmail(email) } returns subscriber

            subscriberService.unsubscribe(email)

            assertFalse(subscriber.isActive)
        }

        @Test
        fun `존재하지 않는 이메일이면 NotFoundException을 던진다`() {
            every { subscriberRepository.findByEmail(email) } returns null

            val ex = assertThrows(NotFoundException::class.java) {
                subscriberService.unsubscribe(email)
            }
            assertNotNull(ex.message)
            assertTrue(ex.message.contains(email))
        }
    }

    @Nested
    @DisplayName("isAlreadyActive")
    inner class IsAlreadyActive {

        @Test
        fun `활성 구독자면 true를 반환한다`() {
            every { subscriberRepository.findByEmail(email) } returns Subscriber(email = email, isActive = true)

            assertTrue(subscriberService.isAlreadyActive(email))
        }

        @Test
        fun `비활성 구독자면 false를 반환한다`() {
            every { subscriberRepository.findByEmail(email) } returns Subscriber(email = email, isActive = false)

            assertFalse(subscriberService.isAlreadyActive(email))
        }

        @Test
        fun `존재하지 않는 이메일이면 false를 반환한다`() {
            every { subscriberRepository.findByEmail(email) } returns null

            assertFalse(subscriberService.isAlreadyActive(email))
        }
    }
}

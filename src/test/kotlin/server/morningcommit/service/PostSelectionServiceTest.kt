package server.morningcommit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import server.morningcommit.domain.PostSendHistory
import server.morningcommit.domain.Subscriber
import server.morningcommit.repository.PostSendHistoryRepository

@ExtendWith(MockKExtension::class)
@DisplayName("PostSelectionService (Shuffle-and-Deplete)")
class PostSelectionServiceTest {

    @MockK
    private lateinit var postSendHistoryRepository: PostSendHistoryRepository

    @InjectMockKs
    private lateinit var postSelectionService: PostSelectionService

    private val subscriber = Subscriber(email = "test@example.com", id = 1L)

    @Test
    fun `전체 포스트 집합이 비어있으면 null을 반환한다`() {
        val result = postSelectionService.selectNextPostId(subscriber, emptySet())

        assertNull(result)
        verify(exactly = 0) { postSendHistoryRepository.findPostIdsBySubscriberId(any()) }
    }

    @Test
    fun `구독자 id가 null이면 null을 반환한다`() {
        val noIdSubscriber = Subscriber(email = "test@example.com", id = null)

        val result = postSelectionService.selectNextPostId(noIdSubscriber, setOf(1L, 2L))

        assertNull(result)
    }

    @Test
    fun `아직 보내지 않은 포스트 중에서만 선택한다`() {
        val allPosts = setOf(1L, 2L, 3L, 4L, 5L)
        val sentPosts = setOf(1L, 2L, 3L)
        every { postSendHistoryRepository.findPostIdsBySubscriberId(1L) } returns sentPosts
        val savedSlot = slot<PostSendHistory>()
        every { postSendHistoryRepository.save(capture(savedSlot)) } answers { firstArg() }

        val result = postSelectionService.selectNextPostId(subscriber, allPosts)

        assertNotNull(result)
        assertTrue(result in setOf(4L, 5L), "보내지 않은 포스트(4, 5) 중 하나여야 함: $result")
        assertEquals(result, savedSlot.captured.postId)
        verify(exactly = 0) { postSendHistoryRepository.deleteAllBySubscriberId(any()) }
    }

    @Test
    fun `모든 포스트를 이미 보냈다면 히스토리를 리셋하고 전체에서 선택한다`() {
        val allPosts = setOf(10L, 20L)
        every { postSendHistoryRepository.findPostIdsBySubscriberId(1L) } returns allPosts
        every { postSendHistoryRepository.deleteAllBySubscriberId(1L) } just Runs
        every { postSendHistoryRepository.save(any<PostSendHistory>()) } answers { firstArg() }

        val result = postSelectionService.selectNextPostId(subscriber, allPosts)

        assertNotNull(result)
        assertTrue(result in allPosts, "리셋 후 전체에서 선택해야 함: $result")
        verify(exactly = 1) { postSendHistoryRepository.deleteAllBySubscriberId(1L) }
        verify(exactly = 1) { postSendHistoryRepository.save(any<PostSendHistory>()) }
    }

    @Test
    fun `선택된 포스트는 반드시 히스토리에 저장된다`() {
        val allPosts = setOf(100L)
        every { postSendHistoryRepository.findPostIdsBySubscriberId(1L) } returns emptySet()
        val savedSlot = slot<PostSendHistory>()
        every { postSendHistoryRepository.save(capture(savedSlot)) } answers { firstArg() }

        val result = postSelectionService.selectNextPostId(subscriber, allPosts)

        assertEquals(100L, result)
        assertEquals(100L, savedSlot.captured.postId)
        assertEquals(subscriber, savedSlot.captured.subscriber)
    }
}

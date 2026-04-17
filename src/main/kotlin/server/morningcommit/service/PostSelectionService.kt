package server.morningcommit.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.domain.PostSendHistory
import server.morningcommit.domain.Subscriber
import server.morningcommit.repository.PostSendHistoryRepository

@Service
class PostSelectionService(
    private val postSendHistoryRepository: PostSendHistoryRepository
) {

    @Transactional
    fun selectNextPostId(subscriber: Subscriber, allPostIdSet: Set<Long>): Long? {
        if (allPostIdSet.isEmpty()) return null

        val subscriberId = subscriber.id ?: return null
        val sentPostIds = postSendHistoryRepository.findPostIdsBySubscriberId(subscriberId)
        var candidates = allPostIdSet - sentPostIds

        if (candidates.isEmpty()) {
            postSendHistoryRepository.deleteAllBySubscriberId(subscriberId)
            candidates = allPostIdSet
        }

        val selectedPostId = candidates.random()
        postSendHistoryRepository.save(PostSendHistory(subscriber = subscriber, postId = selectedPostId))
        return selectedPostId
    }
}

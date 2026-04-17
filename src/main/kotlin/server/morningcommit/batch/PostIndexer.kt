package server.morningcommit.batch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import server.morningcommit.config.RedisConfig
import server.morningcommit.domain.Post
import server.morningcommit.repository.PostRepository
import server.morningcommit.service.PostSearchService

@Component
class PostIndexer(
    private val postRepository: PostRepository,
    private val cacheManager: CacheManager,
    private val postSearchService: PostSearchService
) {
    private val log = KotlinLogging.logger {}

    fun indexAll(posts: List<Post>) {
        if (posts.isEmpty()) return

        val savedPosts = postRepository.saveAll(posts)
        cacheManager.getCache(RedisConfig.POST_LISTING)?.clear()

        try {
            postSearchService.indexPosts(savedPosts.toList())
        } catch (e: Exception) {
            log.error(e) { "Failed to index posts to Elasticsearch" }
        }

        log.info { "Saved ${posts.size} posts" }
    }
}

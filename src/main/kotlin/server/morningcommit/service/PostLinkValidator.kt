package server.morningcommit.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import server.morningcommit.config.RedisConfig
import server.morningcommit.repository.PostRepository

@Service
class PostLinkValidator(
    private val postRepository: PostRepository
) {

    @Cacheable(cacheNames = [RedisConfig.POST_LINK_EXISTS], key = "#url", unless = "!#result")
    fun exists(url: String): Boolean = postRepository.existsByLink(url)
}

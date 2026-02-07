package server.morningcommit.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.config.RedisConfig
import server.morningcommit.config.RestPage
import server.morningcommit.domain.Blog
import server.morningcommit.domain.Post
import server.morningcommit.repository.PostRepository

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository
) {

    @Cacheable(
        cacheNames = [RedisConfig.POST_LISTING],
        key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort.toString()"
    )
    fun findAll(pageable: Pageable): RestPage<Post> {
        val page = postRepository.findAll(pageable)
        return RestPage(page.content, page.number, page.size, page.totalElements)
    }

    @Cacheable(
        cacheNames = [RedisConfig.POST_LISTING],
        key = "'blog:' + #blog.name() + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort.toString()"
    )
    fun findByBlog(blog: Blog, pageable: Pageable): RestPage<Post> {
        val page = postRepository.findByBlog(blog, pageable)
        return RestPage(page.content, page.number, page.size, page.totalElements)
    }

    fun findAllIds(): List<Long> {
        return postRepository.findAllIds()
    }
}

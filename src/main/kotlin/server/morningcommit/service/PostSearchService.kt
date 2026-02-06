package server.morningcommit.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import server.morningcommit.domain.Blog
import server.morningcommit.domain.Post
import server.morningcommit.domain.PostDocument
import server.morningcommit.repository.PostSearchRepository

@Service
class PostSearchService(
    private val postSearchRepository: PostSearchRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(keyword: String, blog: Blog?, pageable: Pageable): Page<PostDocument> {
        return if (blog != null) {
            postSearchRepository.searchByKeywordAndBlog(keyword, blog.name, pageable)
        } else {
            postSearchRepository.searchByKeyword(keyword, pageable)
        }
    }

    fun indexPosts(posts: List<Post>) {
        val documents = posts.map { PostDocument.from(it) }

        postSearchRepository.saveAll(documents)
    }
}

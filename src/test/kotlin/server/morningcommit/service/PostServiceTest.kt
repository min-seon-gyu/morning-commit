package server.morningcommit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import server.morningcommit.domain.Blog
import server.morningcommit.domain.Post
import server.morningcommit.repository.PostRepository

@ExtendWith(MockKExtension::class)
class PostServiceTest {

    @MockK
    private lateinit var postRepository: PostRepository

    @InjectMockKs
    private lateinit var postService: PostService

    private fun post(id: Long, blog: Blog = Blog.KAKAO_TECH, title: String = "제목 $id"): Post =
        Post(title = title, link = "https://example.com/$id", blog = blog, id = id)

    @Nested
    @DisplayName("findAll")
    inner class FindAll {

        @Test
        fun `페이지네이션 결과를 RestPage로 감싸 반환한다`() {
            val pageable = PageRequest.of(0, 9)
            val posts = listOf(post(1), post(2), post(3))
            every { postRepository.findAll(pageable) } returns PageImpl(posts, pageable, 25)

            val result = postService.findAll(pageable)

            assertEquals(3, result.content.size)
            assertEquals(0, result.number)
            assertEquals(9, result.size)
            assertEquals(25, result.totalElements)
            assertEquals("제목 1", result.content[0].title)
        }

        @Test
        fun `포스트가 없으면 빈 RestPage를 반환한다`() {
            val pageable = PageRequest.of(0, 9)
            every { postRepository.findAll(pageable) } returns PageImpl(emptyList<Post>(), pageable, 0)

            val result = postService.findAll(pageable)

            assertTrue(result.content.isEmpty())
            assertEquals(0, result.totalElements)
        }
    }

    @Nested
    @DisplayName("findByBlog")
    inner class FindByBlog {

        @Test
        fun `특정 블로그의 포스트만 필터링해 반환한다`() {
            val pageable = PageRequest.of(0, 9)
            val posts = listOf(
                post(1, Blog.TOSS_TECH, "토스 포스트 1"),
                post(2, Blog.TOSS_TECH, "토스 포스트 2")
            )
            every { postRepository.findByBlog(Blog.TOSS_TECH, pageable) } returns PageImpl(posts, pageable, 2)

            val result = postService.findByBlog(Blog.TOSS_TECH, pageable)

            assertEquals(2, result.content.size)
            assertTrue(result.content.all { it.blog == Blog.TOSS_TECH })
            verify(exactly = 1) { postRepository.findByBlog(Blog.TOSS_TECH, pageable) }
        }

        @Test
        fun `다른 블로그 조회는 서로 독립적으로 리포지토리를 호출한다`() {
            val pageable: Pageable = PageRequest.of(0, 9)
            every { postRepository.findByBlog(Blog.KAKAO_TECH, pageable) } returns
                PageImpl(listOf(post(1, Blog.KAKAO_TECH)), pageable, 1)
            every { postRepository.findByBlog(Blog.WOOWA_BROS, pageable) } returns
                PageImpl(emptyList<Post>(), pageable, 0)

            postService.findByBlog(Blog.KAKAO_TECH, pageable)
            postService.findByBlog(Blog.WOOWA_BROS, pageable)

            verify(exactly = 1) { postRepository.findByBlog(Blog.KAKAO_TECH, pageable) }
            verify(exactly = 1) { postRepository.findByBlog(Blog.WOOWA_BROS, pageable) }
        }
    }

    @Nested
    @DisplayName("findAllIds")
    inner class FindAllIds {

        @Test
        fun `저장된 모든 포스트 ID를 반환한다`() {
            every { postRepository.findAllIds() } returns listOf(1L, 2L, 3L, 100L)

            val ids = postService.findAllIds()

            assertEquals(listOf(1L, 2L, 3L, 100L), ids)
        }

        @Test
        fun `포스트가 없으면 빈 리스트를 반환한다`() {
            every { postRepository.findAllIds() } returns emptyList()

            val ids = postService.findAllIds()

            assertTrue(ids.isEmpty())
        }
    }
}

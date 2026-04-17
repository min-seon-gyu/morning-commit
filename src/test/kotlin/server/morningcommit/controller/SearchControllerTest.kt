package server.morningcommit.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import server.morningcommit.domain.Blog
import server.morningcommit.domain.PostDocument
import server.morningcommit.service.PostSearchService
import java.time.LocalDateTime

@WebMvcTest(SearchController::class)
class SearchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var postSearchService: PostSearchService

    private val now = LocalDateTime.of(2026, 4, 17, 10, 0)

    private fun document(id: Long) = PostDocument(
        id = id.toString(), title = "제목 $id", link = "https://example.com/$id",
        publishDate = now, blog = Blog.KAKAO_TECH.name, createdAt = now, updatedAt = now
    )

    @Test
    fun `GET search - 키워드 없이 호출하면 search 뷰를 반환하고 전체 조회`() {
        every { postSearchService.search(any(), any(), any()) } returns
            PageImpl(listOf(document(1)), Pageable.ofSize(9), 1)

        mockMvc.perform(get("/search"))
            .andExpect(status().isOk)
            .andExpect(view().name("search"))
            .andExpect(model().attributeExists("posts", "keyword", "blogs", "totalResults"))
    }

    @Test
    fun `GET search - 키워드와 블로그 필터가 서비스로 전달된다`() {
        every { postSearchService.search(any(), any(), any()) } returns
            PageImpl(emptyList<PostDocument>(), Pageable.ofSize(9), 0)

        mockMvc.perform(get("/search").param("keyword", "Kotlin").param("blog", "KAKAO_TECH"))
            .andExpect(status().isOk)

        verify { postSearchService.search("Kotlin", Blog.KAKAO_TECH, any()) }
    }

    @Test
    fun `GET search - 잘못된 Blog enum 값이면 C001 반환`() {
        mockMvc.perform(get("/search").param("blog", "NOT_A_BLOG"))
            .andExpect(status().isBadRequest)
    }
}

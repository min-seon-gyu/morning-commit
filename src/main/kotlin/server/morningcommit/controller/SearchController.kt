package server.morningcommit.controller

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import server.morningcommit.domain.Blog
import server.morningcommit.service.PostSearchService

@Controller
class SearchController(
    private val postSearchService: PostSearchService
) {

    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String,
        @RequestParam(required = false) blog: Blog?,
        @PageableDefault(size = 9, sort = ["publishDate"], direction = Sort.Direction.DESC) pageable: Pageable,
        model: Model
    ): String {
        val results = postSearchService.search(keyword, blog, pageable)

        model.addAttribute("posts", results)
        model.addAttribute("keyword", keyword)
        model.addAttribute("blogs", Blog.entries)
        model.addAttribute("currentBlog", blog)
        model.addAttribute("totalResults", results.totalElements)

        return "search"
    }
}

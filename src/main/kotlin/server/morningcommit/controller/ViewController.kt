package server.morningcommit.controller

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import server.morningcommit.domain.Blog
import server.morningcommit.repository.PostRepository

@Controller
class ViewController(
    private val postRepository: PostRepository
) {

    @GetMapping("/")
    fun index(
        @PageableDefault(size = 9, sort = ["publishDate"], direction = Sort.Direction.DESC) pageable: Pageable,
        @RequestParam(required = false) blog: Blog?,
        model: Model
    ): String {
        val posts = if (blog != null) {
            postRepository.findByBlog(blog, pageable)
        } else {
            postRepository.findAll(pageable)
        }

        model.addAttribute("posts", posts)
        model.addAttribute("blogs", Blog.entries)
        model.addAttribute("currentBlog", blog)
        return "index"
    }
}

package server.morningcommit.controller

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import server.morningcommit.repository.PostRepository

@Controller
class ViewController(
    private val postRepository: PostRepository
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val posts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "publishDate"))
        model.addAttribute("posts", posts)
        return "index"
    }
}

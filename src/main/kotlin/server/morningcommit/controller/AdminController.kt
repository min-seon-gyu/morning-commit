package server.morningcommit.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.morningcommit.service.PostSearchService

@RestController
@RequestMapping("/api/admin/search")
class AdminController(
    private val postSearchService: PostSearchService
) {

    @PostMapping("/reindex")
    fun reindex(): ResponseEntity<Map<String, Any>> {
        val count = postSearchService.reindexAll()
        return ResponseEntity.ok(mapOf("indexed" to count))
    }

    @DeleteMapping("/index")
    fun deleteAll(): ResponseEntity<Map<String, Any>> {
        val count = postSearchService.deleteAll()
        return ResponseEntity.ok(mapOf("deleted" to count))
    }
}

package server.morningcommit.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.morningcommit.domain.BlogSource
import server.morningcommit.repository.BlogSourceRepository

@Service
@Transactional(readOnly = true)
class BlogSourceService(
    private val blogSourceRepository: BlogSourceRepository
) {

    fun findActiveSources(): List<BlogSource> {
        return blogSourceRepository.findByIsActiveTrue()
    }
}

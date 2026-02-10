package server.morningcommit.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import server.morningcommit.domain.PostDocument

interface PostSearchRepository : ElasticsearchRepository<PostDocument, String> {

    @Query("""
        {
            "multi_match": {
                "query": "?0",
                "fields": ["title^3", "summary", "tags^2"],
                "type": "best_fields"
            }
        }
    """)
    fun searchByKeyword(keyword: String, pageable: Pageable): Page<PostDocument>

    @Query("""
        {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "?0",
                            "fields": ["title^3", "summary", "tags^2"],
                            "type": "best_fields"
                        }
                    },
                    {
                        "term": {
                            "blog": "?1"
                        }
                    }
                ]
            }
        }
    """)
    fun searchByKeywordAndBlog(keyword: String, blog: String, pageable: Pageable): Page<PostDocument>

    @Query("""
        {
            "match_all": {}
        }
    """)
    fun findAllDocuments(pageable: Pageable): Page<PostDocument>

    @Query("""
        {
            "term": {
                "blog": "?0"
            }
        }
    """)
    fun findAllByBlog(blog: String, pageable: Pageable): Page<PostDocument>
}

package server.morningcommit.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.Setting
import java.time.LocalDateTime

@Document(indexName = "posts")
@Setting(shards = 1, replicas = 0)
data class PostDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val title: String,

    @Field(type = FieldType.Keyword)
    val link: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val description: String?,

    @Field(type = FieldType.Date)
    val publishDate: LocalDateTime?,

    @Field(type = FieldType.Keyword)
    val blog: String,

    @Field(type = FieldType.Date)
    val createdAt: LocalDateTime,

    @Field(type = FieldType.Date)
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostDocument {
            return PostDocument(
                id = post.id.toString(), title = post.title, link = post.link, description = post.description,
                publishDate = post.publishDate, blog = post.blog.name, createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

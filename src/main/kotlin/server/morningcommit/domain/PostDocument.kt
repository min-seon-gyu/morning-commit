package server.morningcommit.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
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
    val summary: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val tags: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val difficulty: String = "INTERMEDIATE",

    @Field(type = FieldType.Integer)
    val readingTimeMin: Int = 0,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second, DateFormat.date])
    val publishDate: LocalDateTime?,

    @Field(type = FieldType.Keyword)
    val blog: String,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second, DateFormat.date])
    val createdAt: LocalDateTime,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second, DateFormat.date])
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostDocument {
            return PostDocument(
                id = post.id.toString(),
                title = post.title,
                link = post.link,
                summary = post.summary,
                tags = post.tags,
                difficulty = post.difficulty,
                readingTimeMin = post.readingTimeMin,
                publishDate = post.publishDate,
                blog = post.blog.name,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

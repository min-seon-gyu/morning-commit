package server.morningcommit.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.morningcommit.domain.BlogSource
import server.morningcommit.domain.Post
import server.morningcommit.service.BlogSourceService
import java.time.LocalDateTime

@Configuration
class BlogCrawlingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val blogSourceService: BlogSourceService,
    private val blogCrawlingService: BlogCrawlingService,
    private val postIndexer: PostIndexer
) {

    @Bean
    fun blogCrawlingJob(): Job {
        return JobBuilder("blogCrawlingJob", jobRepository)
            .start(crawlingStep())
            .build()
    }

    @Bean
    fun crawlingStep(): Step {
        return StepBuilder("crawlingStep", jobRepository)
            .chunk<BlogSource, List<Post>>(1, transactionManager)
            .reader(blogSourceReader())
            .processor(blogSourceProcessor())
            .writer(postListWriter())
            .build()
    }

    @Bean
    @StepScope
    fun blogSourceReader(): ItemReader<BlogSource> {
        val sources = mutableListOf<BlogSource>()
        var initialized = false

        return ItemReader<BlogSource> {
            if (!initialized) {
                sources.addAll(blogSourceService.findActiveSources())
                initialized = true
            }
            sources.removeFirstOrNull()
        }
    }

    @Bean
    @StepScope
    fun blogSourceProcessor(): ItemProcessor<BlogSource, List<Post>> {
        val cutoff = LocalDateTime.now().minusDays(2)
        return ItemProcessor { blogSource ->
            blogCrawlingService.processSource(blogSource, cutoff)
        }
    }

    @Bean
    fun postListWriter(): ItemWriter<List<Post>> {
        return ItemWriter { chunk: Chunk<out List<Post>> ->
            postIndexer.indexAll(chunk.items.flatten())
        }
    }
}

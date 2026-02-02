package server.morningcommit.batch

import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.morningcommit.domain.PostSendHistory
import server.morningcommit.domain.Subscriber
import server.morningcommit.email.EmailProducer
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.service.PostService

@Configuration
class EmailDeliveryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val postService: PostService,
    private val emailProducer: EmailProducer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun emailDeliveryJob(): Job {
        return JobBuilder("emailDeliveryJob", jobRepository)
            .start(emailDeliveryStep())
            .build()
    }

    @Bean
    fun emailDeliveryStep(): Step {
        return StepBuilder("emailDeliveryStep", jobRepository)
            .chunk<Subscriber, EmailRequest>(10, transactionManager)
            .reader(subscriberReader())
            .processor(subscriberToEmailRequestProcessor())
            .writer(emailRequestWriter())
            .build()
    }

    @Bean
    @StepScope
    fun subscriberReader(): JpaPagingItemReader<Subscriber> {
        return JpaPagingItemReaderBuilder<Subscriber>()
            .name("subscriberReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT s FROM Subscriber s WHERE s.isActive = true")
            .pageSize(10)
            .build()
    }

    @Bean
    @StepScope
    fun subscriberToEmailRequestProcessor(): ItemProcessor<Subscriber, EmailRequest> {
        val allPostIdSet = postService.findAllIds().toSet()

        return ItemProcessor { subscriber ->
            if (allPostIdSet.isEmpty()) {
                return@ItemProcessor null
            }

            val sentPostIds = subscriber.sendHistories.map { it.postId }.toSet()

            var candidates = allPostIdSet - sentPostIds

            if (candidates.isEmpty()) {
                subscriber.sendHistories.clear()

                candidates = allPostIdSet
            }

            val selectedPostId = candidates.random()
            subscriber.sendHistories.add(PostSendHistory(subscriber = subscriber, postId = selectedPostId))

            EmailRequest(subscriberId = subscriber.id!!, email = subscriber.email, postIds = listOf(selectedPostId))
        }
    }

    @Bean
    fun emailRequestWriter(): ItemWriter<EmailRequest> {
        return ItemWriter { chunk ->
            chunk.items.forEach { emailRequest ->
                try {
                    emailProducer.sendEmailEvent(emailRequest)

                    log.info("Queued email for: ${emailRequest.email}")
                } catch (e: Exception) {
                    log.error("Failed to queue email for ${emailRequest.email}: ${e.message}", e)
                }
            }
        }
    }
}

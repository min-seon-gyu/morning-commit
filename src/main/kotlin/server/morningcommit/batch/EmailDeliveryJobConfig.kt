package server.morningcommit.batch

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManagerFactory
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
import server.morningcommit.domain.Subscriber
import server.morningcommit.email.EmailProducer
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.service.PostSelectionService
import server.morningcommit.service.PostService

@Configuration
class EmailDeliveryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val postService: PostService,
    private val emailProducer: EmailProducer,
    private val postSelectionService: PostSelectionService
) {
    private val log = KotlinLogging.logger {}

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
            val selectedPostId = postSelectionService.selectNextPostId(subscriber, allPostIdSet)
                ?: return@ItemProcessor null

            EmailRequest(subscriberId = subscriber.id!!, email = subscriber.email, postId = selectedPostId)
        }
    }

    @Bean
    fun emailRequestWriter(): ItemWriter<EmailRequest> {
        return ItemWriter { chunk ->
            chunk.items.forEach { emailRequest ->
                try {
                    emailProducer.sendEmailEvent(emailRequest)

                    log.info { "Queued email for: ${emailRequest.email}" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to queue email for ${emailRequest.email}: ${e.message}" }
                }
            }
        }
    }
}

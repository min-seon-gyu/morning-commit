package server.morningcommit.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.morningcommit.domain.PostSendHistory
import server.morningcommit.domain.Subscriber
import server.morningcommit.email.EmailProducer
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.repository.PostRepository
import server.morningcommit.repository.PostSendHistoryRepository
import server.morningcommit.repository.SubscriberRepository

@Configuration
class EmailDeliveryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val subscriberRepository: SubscriberRepository,
    private val postRepository: PostRepository,
    private val postSendHistoryRepository: PostSendHistoryRepository,
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
    fun subscriberReader(): ItemReader<Subscriber> {
        val subscribers = mutableListOf<Subscriber>()
        var initialized = false

        return ItemReader {
            if (!initialized) {
                subscribers.addAll(subscriberRepository.findByIsActiveTrue())
                initialized = true
            }
            subscribers.removeFirstOrNull()
        }
    }

    @Bean
    fun subscriberToEmailRequestProcessor(): ItemProcessor<Subscriber, EmailRequest> {
        return ItemProcessor { subscriber ->
            val userId = subscriber.id!!

            val allPostIds = postRepository.findAll().mapNotNull { it.id }

            if (allPostIds.isEmpty()) {
                log.info("No posts available. Skipping email for: ${subscriber.email}")

                return@ItemProcessor null
            }

            val sentPostIds = postSendHistoryRepository.findSentPostIdsByUserId(userId).toSet()

            var candidates = allPostIds.filter { it !in sentPostIds }

            if (candidates.isEmpty()) {
                postSendHistoryRepository.deleteByUserId(userId)
                candidates = allPostIds
            }

            val selectedPostId = candidates.random()

            postSendHistoryRepository.save(PostSendHistory(userId = userId, postId = selectedPostId))

            EmailRequest(subscriberId = userId, email = subscriber.email, postIds = listOf(selectedPostId))
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

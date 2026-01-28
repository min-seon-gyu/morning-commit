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
import server.morningcommit.domain.Subscriber
import server.morningcommit.email.EmailProducer
import server.morningcommit.email.dto.EmailRequest
import server.morningcommit.repository.PostRepository
import server.morningcommit.repository.SubscriberRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Configuration
class EmailDeliveryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val subscriberRepository: SubscriberRepository,
    private val postRepository: PostRepository,
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
            val startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
            val todayPosts = postRepository.findTodayPosts(startOfDay)

            if (todayPosts.isEmpty()) {
                log.info("No posts collected today. Skipping email for: ${subscriber.email}")
                return@ItemProcessor null
            }

            val postIds = todayPosts.mapNotNull { it.id }
            log.info("Creating email request for ${subscriber.email} with ${postIds.size} posts")

            EmailRequest(
                subscriberId = subscriber.id!!,
                email = subscriber.email,
                postIds = postIds
            )
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

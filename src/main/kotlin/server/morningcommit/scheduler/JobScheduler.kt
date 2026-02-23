package server.morningcommit.scheduler

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("blogCrawlingJob") private val blogCrawlingJob: Job,
    @Qualifier("emailDeliveryJob") private val emailDeliveryJob: Job
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 1 * * *")
    fun runCrawlingJob() {
        try {
            val crawlingParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val crawlingExecution = jobLauncher.run(blogCrawlingJob, crawlingParams)

            if (crawlingExecution.status.isUnsuccessful) {
                log.error("blogCrawlingJob failed.")

                return
            }

            log.info("=== Blog Crawling Job Completed ===")
        } catch (e: Exception) {
            log.error("Blog crawling job failed: ${e.message}", e)
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    fun runEmailDeliveryJob() {
        try {
            val deliveryParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val deliveryExecution = jobLauncher.run(emailDeliveryJob, deliveryParams)

            if (deliveryExecution.status.isUnsuccessful) {
                log.error("emailDeliveryJob failed.")

                return
            }

            log.info("=== Email Delivery Job Completed ===")
        } catch (e: Exception) {
            log.error("Email delivery job failed: ${e.message}", e)
        }
    }
}

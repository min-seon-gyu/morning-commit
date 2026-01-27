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

    @Scheduled(cron = "0 0 7 * * *")
    fun runDailyNewsletter() {
        log.info("=== Starting Daily Newsletter Job ===")

        try {
            // Step 1: Run blog crawling job
            log.info("Starting blogCrawlingJob...")
            val crawlingParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val crawlingExecution = jobLauncher.run(blogCrawlingJob, crawlingParams)

            if (crawlingExecution.status.isUnsuccessful) {
                log.error("blogCrawlingJob failed. Skipping email delivery.")
                return
            }
            log.info("blogCrawlingJob completed successfully.")

            // Step 2: Run email delivery job
            log.info("Starting emailDeliveryJob...")
            val deliveryParams = JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val deliveryExecution = jobLauncher.run(emailDeliveryJob, deliveryParams)

            if (deliveryExecution.status.isUnsuccessful) {
                log.error("emailDeliveryJob failed.")
                return
            }
            log.info("emailDeliveryJob completed successfully.")

            log.info("=== Daily Newsletter Job Completed ===")
        } catch (e: Exception) {
            log.error("Daily newsletter job failed: ${e.message}", e)
        }
    }
}

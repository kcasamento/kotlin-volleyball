package kac.crawl

import com.sun.management.jmx.Trace.send
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kac.broker.dsl.KafkaDSL
import kac.broker.dsl.kafka
import kac.crawl.action.*
import kac.crawl.event.CrawlJobCompleted
import kac.crawl.event.CrawlJobStarted
import kac.crawl.model.CrawlJob
import kac.crawl.repository.CrawlJobMongoRepository
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class CrawlHandler(
    private val crawlRepository: CrawlJobMongoRepository,
    private val httpClient: HttpClient,
    private val broker: KafkaDSL,
    private val crawlEventTopic: String
) {

    suspend fun handle(action: GetCrawlJobs): List<CrawlJob> {
        return crawlRepository.getJobs()
    }

    suspend fun handle(action: GetCrawlJobById): CrawlJob {
        return crawlRepository.getJobById(ObjectId(action.id))
    }

    suspend fun handle(action: GetMostRecentCrawlJobs): List<CrawlJob> {
        return crawlRepository.getMostRecentJobs(action.limit)
    }

    suspend fun handle(action: StartCrawlJob): CrawlJob {

        return broker.producerAsync<CrawlJob>(crawlEventTopic) {

            try {

                val newJob = crawlRepository.startJob()

                val event = CrawlJobStarted(newJob.id.toHexString(), newJob.startTimestamp)

                send(event).get()

                newJob

            } finally {

                flush()

            }
        }


    }

    suspend fun handle(action: CompleteCrawlJob): CrawlJob {

        val job = crawlRepository.completeJob(ObjectId(action.id), action.message)

        broker.producer(crawlEventTopic) {

            try {

                val event = CrawlJobCompleted(
                    job.id.toHexString(),
                    action.message,
                    job.completeTimestamp?:
                    Date.from(LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)))

                send(event).get()

            } finally {

                flush()

            }


        }

        return job

    }

    suspend fun handle(action: FailCrawlJob): CrawlJob {
        return crawlRepository.failJob(ObjectId(action.id), action.errorMessage)
    }

}
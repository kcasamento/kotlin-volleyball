package kac.crawl.repository


import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import kac.crawl.model.CrawlJob
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineClient
import java.lang.IllegalArgumentException
import java.util.*

class CrawlJobMongoRepository(
    dbClient: CoroutineClient,
    databaseName: String,
    collectionName: String) {

    private val database = dbClient.getDatabase(databaseName)
    private val crawlJobs = database.getCollection<CrawlJob>(collectionName)

    suspend fun getJobById(id: ObjectId): CrawlJob {

        val job = this.crawlJobs.findOneById(id)

        return checkNotNull(job)

    }

    suspend fun getJobs(): List<CrawlJob> {

        return this.crawlJobs.find().toList()

    }

    suspend fun getMostRecentJobs(limit: Int): List<CrawlJob> {

        val jobs = this.crawlJobs
            .find(Filters.eq("status", "COMPLETED"))
            .limit(limit)
            .sort(Sorts.descending("completeTimestamp"))
            .toList()

        return jobs

    }

    suspend fun startJob(): CrawlJob {

        val newJob = CrawlJob.startJob()

        this.crawlJobs.insertOne(newJob)

        return newJob

    }

    suspend fun completeJob(id: ObjectId, message: String?): CrawlJob {

        val job = this.getJobById(id)

        try {
            return this.updateJob(job.completeJob(message))
        } catch (ex: IllegalArgumentException) {
            println("$ex")
        }

        return job

    }

    suspend fun failJob(id: ObjectId, errorMessage: String): CrawlJob {

        val job = this.getJobById(id)

        return this.updateJob(job.failJob(errorMessage))

    }

    suspend fun resetDb() {
        this.crawlJobs.deleteMany()
    }

    private suspend fun updateJob(updatedJob: CrawlJob): CrawlJob {

        this.crawlJobs.replaceOneById(updatedJob.id, updatedJob)

        return updatedJob

    }

}
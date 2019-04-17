package kac.crawl.model

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.lang.IllegalArgumentException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

enum class CrawlState(val status: String) {
    RUNNING("RUNNING"),
    COMPLETED( "COMPLETED"),
    FAILED("FAILED")
}

data class CrawlJob (
    @BsonId val id: ObjectId,
    val startTimestamp: Date,
    val status: CrawlState,
    val message: String,
    val completeTimestamp: Date? = null
) {

    companion object {

        fun startJob(): CrawlJob {

            val instant = LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)

            val id = ObjectId()
            val timestamp = Timestamp.from(instant)
            return CrawlJob(id, timestamp, CrawlState.RUNNING, "")

        }

    }

    @Throws(IllegalArgumentException::class)
    fun completeJob(message: String?): CrawlJob {

//        if(this.status != CrawlState.RUNNING)
//            throw IllegalArgumentException("Job is not running.")

        val instant = LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)

        return this.copy(
            status = CrawlState.COMPLETED,
            message = message ?: "",
            completeTimestamp = Timestamp.from(instant))
    }

    fun failJob(errorMessage: String): CrawlJob {

        if(this.status != CrawlState.RUNNING)
            throw IllegalArgumentException("Job is not running.")

        val instant = LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)

        return this.copy(
            status = CrawlState.FAILED,
            message = errorMessage,
            completeTimestamp = Timestamp.from(instant))
    }

}
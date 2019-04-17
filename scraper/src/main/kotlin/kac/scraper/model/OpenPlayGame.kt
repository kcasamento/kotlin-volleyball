package kac.scraper.model

import org.bson.types.ObjectId
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class OpenPlayGame (
    val gameId: String,
    val correlationId: ObjectId,
    val level: String,
    val gym: String,
    val gameDate: String,
    val soldOut: Boolean,
    val record_date: Date = Timestamp.from(LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC))
) {

}
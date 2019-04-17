package kac.transformer.repository

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.Filters
import kac.scraper.model.OpenPlayGame
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.replaceOne
import org.litote.kmongo.upsert

class TransformerRepository(
    dbClient: CoroutineClient,
    databaseName: String,
    collectionName: String
) {

    private val database = dbClient.getDatabase(databaseName)
    private val gamesCollection = database.getCollection<OpenPlayGame>(collectionName)

    suspend fun upsertGames(games: List<OpenPlayGame>): BulkWriteResult {

        val operations = games.map{
            replaceOne(
                Filters.and(
                    Filters.eq("gameId", it.gameId),
                    Filters.eq("correlationId", it.correlationId)),
                it,
                upsert()
            )
        }

        return gamesCollection.bulkWrite(operations)

    }

}
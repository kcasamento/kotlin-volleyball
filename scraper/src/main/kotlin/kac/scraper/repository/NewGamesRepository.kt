package kac.scraper.repository

import kac.scraper.model.OpenPlayGame
import org.litote.kmongo.coroutine.CoroutineClient


class NewGamesRepository(
    dbClient: CoroutineClient,
    databaseName: String,
    collectionName: String
) {

    private val database = dbClient.getDatabase(databaseName)
    private val gamesCollection = database.getCollection<OpenPlayGame>(collectionName)

    suspend fun getNewGames(): List<OpenPlayGame> {

        return gamesCollection
            .find()
            .toList()

    }

}
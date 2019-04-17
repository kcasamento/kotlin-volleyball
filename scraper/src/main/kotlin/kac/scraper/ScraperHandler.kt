package kac.scraper

import kac.broker.dsl.KafkaDSL
import kac.broker.dsl.kafka
import kac.scraper.action.GetGames
import kac.scraper.action.GetNewGames
import kac.scraper.action.ScrapeForNewNYUrbanGames
import kac.scraper.event.NewGamesAdded
import kac.scraper.model.OpenPlayGame
import kac.scraper.repository.*
import kac.scraper.service.ScrapeService
import org.bson.types.ObjectId
import java.util.*

class ScraperHandler(
    private val scrapeRepository: ScrapeGameRepository,
    private val newGamesRepository: NewGamesRepository,
    private val broker: KafkaDSL,
    private val brokerProps: Properties
) {

    suspend fun handle(action: ScrapeForNewNYUrbanGames) {

        ScrapeService(
            scrapeRepository,
            ObjectId(action.corrId),
            action.url,
            action.params
        ).scrapeAndRecordGames()

        broker.producer(brokerProps.getProperty("topic")) {

                val event = NewGamesAdded(action.corrId)
                send(event).get()

                flush()

        }

    }

    suspend fun handle(action: GetGames): List<OpenPlayGame> {
        return scrapeRepository.getGames(ObjectId(action.corrId))
    }

    suspend fun handle(action: GetNewGames): List<OpenPlayGame> {
        return newGamesRepository.getNewGames()
    }

}
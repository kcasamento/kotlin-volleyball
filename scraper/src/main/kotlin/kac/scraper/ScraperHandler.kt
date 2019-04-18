package kac.scraper

import kac.broker.dsl.KafkaDSL
import kac.scraper.action.GetGames
import kac.scraper.action.GetNewGames
import kac.scraper.action.ScrapeForNewNYUrbanGames
import kac.scraper.event.ScapeFinished
import kac.scraper.event.ScraperErrored
import kac.scraper.model.OpenPlayGame
import kac.scraper.repository.*
import kac.scraper.service.ScrapeService
import org.bson.types.ObjectId

class ScraperHandler(
    private val scrapeRepository: ScrapeGameRepository,
    private val newGamesRepository: NewGamesRepository,
    private val broker: KafkaDSL,
    private val scraperEventTopic: String
) {

    suspend fun handle(action: ScrapeForNewNYUrbanGames) {

        broker.producerAsync(scraperEventTopic) {

            try {

                ScrapeService(
                    scrapeRepository,
                    ObjectId(action.corrId),
                    action.url,
                    action.params
                ).scrapeAndRecordGames()

                val event = ScapeFinished(action.corrId)
                send(event).get()


            } catch(ex: Exception) {

                val errorEvent = ScraperErrored(ex, ex.message ?: "")
                send(errorEvent).get()

            } finally {
                flush ()
            }

        }

    }

    suspend fun handle(action: GetGames): List<OpenPlayGame> {
        return scrapeRepository.getGames(ObjectId(action.corrId))
    }

    suspend fun handle(action: GetNewGames): List<OpenPlayGame> {
        return newGamesRepository.getNewGames()
    }

}
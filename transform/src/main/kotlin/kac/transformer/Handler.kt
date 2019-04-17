package kac.transformer

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kac.broker.dsl.KafkaDSL
import kac.crawl.model.CrawlJob
import kac.scraper.event.NewGamesAdded
import kac.scraper.model.OpenPlayGame
import kac.transformer.event.NewGamesOpened
import kac.transformer.repository.TransformerRepository
import java.util.*

class Handler(
    private val httpClient: HttpClient,
    private val repository: TransformerRepository,
    private val broker: KafkaDSL,
    private val brokerProps: Properties
) {

    suspend fun handle(event: NewGamesAdded) {

        println("Handling NewGamesAdded")

        // 1. get last 2 runs - crawl/recent
        val crawlJobs = httpClient
            .get<List<CrawlJob>>("http://crawl-service/crawl/recent/2")

        val currentJob = crawlJobs.getOrNull(0) // 0 = most recent job
        val prevJob = crawlJobs.getOrNull(1) // 1 = second most recent job

        // No job has finished...
        // If this event is raised this should
        // technically never happen
        if(currentJob == null) return

        // Get the games from the most recent (current) crawl job
        val currentGames =  httpClient
            .get<List<OpenPlayGame>>("http://scraper-service/scrape/games/${currentJob.id}")
            .filter {
                !it.soldOut
            }

        var gamesToWrite: List<OpenPlayGame>

        if(prevJob == null) {

            // First run:
            // All games are new
            // write currentGames to DB
            gamesToWrite = currentGames.map{
                it.copy()
            }

        } else {

            // Get games from the previous crawl job
            // to compare against the current
            val previousGames = httpClient
                .get<List<OpenPlayGame>>("http://scraper-service/scrape/games/${prevJob.id}")
                .filter {
                    !it.soldOut
                }

            // Get all games in the list of current open spots that
            // are not in the list of open spots from the last run
            //
            // The difference is new open spots
            val previousGamesSet = previousGames.distinctBy { it.gameId }.toSet()
            val referenceIds = previousGamesSet.map { it.gameId }

            gamesToWrite = currentGames.filter { it.gameId !in referenceIds }

        }

        if(gamesToWrite.count() > 0) {

            repository.upsertGames(gamesToWrite)

            broker.producer(brokerProps.getProperty("topic")) {

                val event = NewGamesOpened(currentJob.id.toHexString())
                send(event).get()

                flush()

            }

        }

    }

}
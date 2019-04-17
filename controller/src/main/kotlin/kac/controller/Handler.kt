package kac.controller

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kac.crawl.event.CrawlJobCompleted
import kac.crawl.model.CrawlJob
import kac.scraper.event.NewGamesAdded

class Handler(private val httpClient: HttpClient) {

    suspend fun handle(event: NewGamesAdded) {
        println("Handling NewGamesAdded")
        httpClient.post<CrawlJob>("http://crawl-service/crawl/${event.corrId}/complete")
    }

    suspend fun handle(event: CrawlJobCompleted) {
        println("Handling ${CrawlJobCompleted::class}")
    }

}
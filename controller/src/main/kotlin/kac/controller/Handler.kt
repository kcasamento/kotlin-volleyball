package kac.controller

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kac.crawl.event.CrawlJobCompleted
import kac.crawl.event.CrawlJobStarted
import kac.crawl.model.CrawlJob
import kac.scraper.event.ScapeFinished

class Handler(private val httpClient: HttpClient) {

    suspend fun handle(event: ScapeFinished) {
        print(event)
        httpClient.post<CrawlJob>("http://crawl-service/crawl/${event.corrId}/complete")
    }

    suspend fun handle(event: CrawlJobCompleted) {
        print(event)
    }

    suspend fun handle(event: CrawlJobStarted) {
        print(event)
        httpClient.post<String>("http://scraper-service/scrape/${event.correlationId}")
    }

    fun <T: Any> print(event: T) {
        println("Handling ${event::class}")
    }

}
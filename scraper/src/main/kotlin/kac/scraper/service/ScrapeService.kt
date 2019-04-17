package kac.scraper.service

import kac.scraper.model.OpenPlayGame
import kac.scraper.repository.ScrapeGameRepository
import org.bson.types.ObjectId
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ScrapeService(
    private val repo: ScrapeGameRepository,
    private val correlationId: ObjectId,
    private val url: String,
    private val dataMapList: List<Map<String, String>>) {

    suspend fun scrapeAndRecordGames() {

        val results = this.dataMapList.map {
            this.repo.upsertGames(this.parseHtml(this.url, it))
        }

    }

    private fun parseHtml(url: String, dataMap: Map<String, String>): List<OpenPlayGame> {

        val document = this.getDocument(url, dataMap)

        return this.parseDocument(document)

    }

    private fun getDocument(url: String, dataMap: Map<String, String>): Document {

        return Jsoup.connect(url)
            .timeout(60000)
            .method(Connection.Method.POST)
            .run {

                dataMap.forEach{
                    this.data(it.key, it.value)
                }

                this

            }
            .execute()
            .parse() ?: Document(url)

    }

    private fun parseDocument(document: Document): List<OpenPlayGame> {

        return document
            .select("table:first-of-type tr:not(:eq(0))")
            .fold(mutableListOf<OpenPlayGame>()) { acc, element ->

                val tds = element.getElementsByTag("td")
                val gameId = tds[0].selectFirst("input.custom_inputs").attr("value")
                val level = tds[3].text().split('-')[0].trim().toLowerCase()
                val soldOut = tds[6].text().trim().toLowerCase().indexOf("sold out") > 0
                val date = tds[1].text().trim().toLowerCase()
                val gym = tds[2].text().trim().toLowerCase()

                acc.add(OpenPlayGame(
                    gameId, this.correlationId, level, gym, date, soldOut
                ))

                acc

            }
            .toList()

    }

}
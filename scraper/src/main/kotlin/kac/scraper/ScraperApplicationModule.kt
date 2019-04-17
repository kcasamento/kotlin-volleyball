package kac.scraper

import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kac.broker.dsl.KafkaDSL
import kac.common.config.BrokerApplicationConfig
import kac.scraper.action.GetGames
import kac.scraper.action.GetNewGames
import kac.scraper.action.ScrapeForNewNYUrbanGames
import kac.scraper.repository.*
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.litote.kmongo.reactivestreams.*
import org.litote.kmongo.coroutine.*

fun Application.main() {

    // Consul Environment
    val consulHost = System.getenv("CONSUL_HOST") ?: "http://localhost"
    val consulPort = System.getenv("CONSUL_PORT") ?: 8500
    val consulUri = "$consulHost:$consulPort"

    // Mongo Environment
    val mongoHost = System.getenv("MONGODB_HOST") ?: "mongodb://localhost"
    val mongoPort = System.getenv("MONGODB_PORT") ?: 27017
    val mongoUri = "$mongoHost:$mongoPort"
    val db = KMongo.createClient(mongoUri).coroutine


    // Bootstrap Repository
    // TODO: put in config or another class
    val nyUrbanUrl = "https://www.nyurban.com/wp-admin/admin-ajax.php"
    val nyUrbanParams = listOf(
        mapOf(
            "action" to "my_open_play_contentbb",
            "buttonid" to "2",
            "gametypeid" to "1",
            "filterid" to "16"),

        mapOf(
            "action" to "my_open_play_contentbb",
            "buttonid" to "1",
            "gametypeid" to "1",
            "filterid" to "34"),

        mapOf(
            "action" to "my_open_play_contentbb",
            "buttonid" to "3",
            "gametypeid" to "1",
            "filterid" to "18")
    )
    val scrapeGamesRepository = ScrapeGameRepository(
        db,
        "web-crawl",
        "scraper-games")

    val newGamesRepository = NewGamesRepository(
        db,
        "web-crawl",
        "new-games"
    )

    // Kafka Environment
    val properties = BrokerApplicationConfig().getProperties()
    val kafkaHosts = System.getenv("BROKER_BOOTSTRAP_SERVERS") ?: properties.getProperty("bootstrap.servers") ?: ""
    val broker = KafkaDSL(kafkaHosts)

    // Bootstrap Handler
    val handler = ScraperHandler(scrapeGamesRepository, newGamesRepository, broker, properties)


    // Bootstrap Ktor Web Server
    install(ContentNegotiation) {
        gson {
            // Configure Gson here
            this.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .registerTypeAdapter(ObjectId::class.java, JsonSerializer<ObjectId> {
                        src, _, _ -> JsonPrimitive(src?.toHexString())
                })
                .registerTypeAdapter(ObjectId::class.java, JsonDeserializer<ObjectId>{
                        json, _, _ -> ObjectId(json.asString)
                })
        }
    }


    routing {

        post("/scrape/{correlationId}") {

            val corrId = checkNotNull(call.parameters["correlationId"])
            val action = ScrapeForNewNYUrbanGames(corrId, nyUrbanUrl, nyUrbanParams)

            // Don't wait to return to caller...
            launch { handler.handle(action) }

            call.respond(message = "Started scraping $nyUrbanUrl.")
        }

        get( "/scrape/games/new") {

            val games = handler.handle(GetNewGames())
            call.respond(message = games)

        }

        get( "/scrape/games/{correlationId}") {

            val corrId = checkNotNull(call.parameters["correlationId"])
            val action = GetGames(corrId)

            val games = handler.handle(action)

            call.respond(message = games)

        }

    }

}
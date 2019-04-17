package kac.crawl

import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kac.broker.dsl.KafkaDSL
import kac.common.config.BrokerApplicationConfig
import kac.common.infrastructure.ConsulFeature
import kac.crawl.action.*
import kac.crawl.repository.CrawlJobMongoRepository
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

    // Bootstrap repository
    val crawlRepository = CrawlJobMongoRepository(db, "web-crawl", "crawl-jobs")

    // Bootstrap Http Client for cross-service communication
    val client = HttpClient(Apache) {

        install(ConsulFeature) {
            consulUrl = consulUri
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }

    }

    // Kafka Environment
    val properties = BrokerApplicationConfig().getProperties()
    val kafkaHosts = System.getenv("BROKER_BOOTSTRAP_SERVERS") ?: properties.getProperty("bootstrap.servers") ?: ""
    val broker = KafkaDSL(kafkaHosts)
    val handler = CrawlHandler(crawlRepository, client, broker, properties)

    install(ContentNegotiation) {
        gson {

            this
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .registerTypeAdapter(ObjectId::class.java, JsonSerializer<ObjectId> {
                        src, _, _ -> JsonPrimitive(src?.toHexString())
                })
                .registerTypeAdapter(ObjectId::class.java, JsonDeserializer<ObjectId>{
                    json, _, _ -> ObjectId(json.asString)
                })

        }
    }

    routing{

        get("/crawl") {

            val jobs = handler.handle(GetCrawlJobs())
            call.respond(message = jobs)

        }

        get("/crawl/{id}") {

            val id = checkNotNull(call.parameters["id"])
            val action = GetCrawlJobById(id)
            val job = handler.handle(action)
            call.respond(message = job)

        }

        get("/crawl/recent/{limit}") {

            val limit = call.parameters["limit"]?: "1"

            val action = GetMostRecentCrawlJobs(limit.toInt())
            val jobs = handler.handle(action)

            call.respond(message = jobs)


        }

        post("/crawl/start") {

            val newJob = handler.handle(StartCrawlJob())
            call.respond(message = newJob)

        }

        post("/crawl/{id}/complete") {
            try {
                val id = checkNotNull(call.parameters["id"])
                val action = CompleteCrawlJob(id, "API triggered completion.")
                val completedJob = handler.handle(action)
                call.respond(message = completedJob)
            } catch (ex: Exception) {
                call.respond(HttpStatusCode.BadRequest, ex.message?:"")
            }
        }

        post("/crawl/{id}/fail") {

            val id = checkNotNull(call.parameters["id"])
            val errorMessage = checkNotNull<String>(call.receive())

            val action = FailCrawlJob(id, errorMessage)

            val failedJob = handler.handle(action)

            call.respond(message = failedJob)

        }

        get("/crawl/flush") {

            crawlRepository.resetDb()
            call.respond(message = "Database has been flushed.")

        }

    }

}
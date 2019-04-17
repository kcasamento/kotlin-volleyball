package kac.transformer

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import javafx.application.Application.launch
import kac.broker.dsl.*
import kac.common.config.BrokerApplicationConfig
import kac.common.infrastructure.ConsulFeature
import kac.scraper.event.NewGamesAdded
import kac.scraper.model.OpenPlayGame
import kac.transformer.repository.TransformerRepository
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.lang.reflect.Type


fun main(args: Array<String>) {

    // Mongo Environment
    val mongoHost = System.getenv("MONGODB_HOST") ?: "mongodb://localhost"
    val mongoPort = System.getenv("MONGODB_PORT") ?: 27017
    val mongoUri = "$mongoHost:$mongoPort"
    val db = KMongo.createClient(mongoUri).coroutine

    val newGamesRepository = TransformerRepository(
        db,
        "web-crawl",
        "new-games")

    val gson = Gson()
    val client = HttpClient(Apache) {

        install(ConsulFeature) {
            consulUrl = "http://localhost:8500"
            local = true
        }

        install(JsonFeature) {
            serializer = GsonSerializer {
                this.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .registerTypeAdapter(ObjectId::class.java, JsonSerializer<ObjectId> { src, _, _ ->
                        JsonPrimitive(src?.toHexString())
                    })
                    .registerTypeAdapter(ObjectId::class.java, JsonDeserializer<ObjectId> { json, _, _ ->
                        ObjectId(json.asString)
                    })
            }

        }
    }


    val properties = BrokerApplicationConfig().getProperties()
    val kafkaHosts = System.getenv("BROKER_BOOTSTRAP_SERVERS") ?: properties.getProperty("bootstrap.servers") ?: ""
    val broker = KafkaDSL(kafkaHosts)

    val handler = Handler(client, newGamesRepository, broker, properties)

    // Listen for NewGamesAdded Event from the scraper service
    broker.consumer(properties.getProperty("topic").split(','), properties.getProperty("groupId")) {

        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            stop()
        }))

        // Start consuming until the process is shutdown
        consume { topic: String, key: String, value: String, type: String ->

            println("New message on: $topic")

            when(type) {
                NewGamesAdded::class.java.toString() -> runBlocking {
                    handler.handle(gson.fromJson<NewGamesAdded>(value, NewGamesAdded::class.java))
                }
            }
        }
    }

}
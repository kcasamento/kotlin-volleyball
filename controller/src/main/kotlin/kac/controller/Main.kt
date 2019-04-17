package kac.controller

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
import kac.crawl.event.CrawlJobCompleted
import kac.scraper.event.NewGamesAdded
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.lang.reflect.Type


fun main(args: Array<String>) {

    val properties = BrokerApplicationConfig().getProperties()
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

    val handler = Handler(client)

    kafka(properties.getProperty("bootstrap.servers")) {
        consumer(properties.getProperty("topic").split(','), properties.getProperty("groupId")) {

            Runtime.getRuntime().addShutdownHook(Thread(Runnable {
                stop()
            }))

            // Start consuming until the process is shutdown
            consume { topic: String, key: String, value: String, type: String ->

                println("New message on: $topic")

                when(type) {
                    NewGamesAdded::class.java.toString() -> runBlocking { handler.handle(gson.fromJson<NewGamesAdded>(value, NewGamesAdded::class.java)) }
                    CrawlJobCompleted::class.java.toString() -> runBlocking {handler.handle(gson.fromJson<CrawlJobCompleted>(value, CrawlJobCompleted::class.java))}
                }

            }
        }
    }

}
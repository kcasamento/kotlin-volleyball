package kac.controller

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import kac.broker.dsl.*
import kac.common.config.BrokerApplicationConfig
import kac.common.infrastructure.ConsulFeature
import kac.crawl.event.CrawlJobCompleted
import kac.crawl.event.CrawlJobStarted
import kac.scraper.event.NewGamesAdded
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.lang.Class


fun main(args: Array<String>) {

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

    val properties = BrokerApplicationConfig().getProperties()
    val kafkaHosts = System.getenv("BROKER_BOOTSTRAP_SERVERS")
        ?: properties.getProperty("bootstrap.servers")
        ?: ""
    val kafkaTopics = properties.getProperty("topics").split(',')
    val groupId = properties.getProperty("groupId")


    kafka(kafkaHosts) {

        consumer(kafkaTopics, groupId) {

            Runtime.getRuntime().addShutdownHook(Thread(Runnable {
                stop()
            }))

            // Start consuming until the process is shutdown
            consume { topic: String, key: String, value: String, type: String ->

                try {

                    val eventType = Class.forName(type)
                    val event = gson.fromJson(value, eventType)


                    println("New message on: $topic")

                    runBlocking {
                        when (eventType) {
                            NewGamesAdded::class.java -> handler.handle(event as NewGamesAdded)

                            CrawlJobCompleted::class.java -> handler.handle(event as CrawlJobCompleted)

                            CrawlJobStarted::class.java -> handler.handle(event as CrawlJobStarted)
                        }
                    }
                }
                catch (typeInvalidError: ClassNotFoundException) {}
                catch (typeInitError: ExceptionInInitializerError) {}
                catch (typeLinkageError: LinkageError) {}

            }

        }

    }

}

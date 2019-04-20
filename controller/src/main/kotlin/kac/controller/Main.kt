package kac.controller

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.BadResponseStatusException
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import kac.broker.dsl.*
import kac.common.config.BrokerApplicationConfig
import kac.common.infrastructure.ConsulFeature
import kac.crawl.event.CrawlJobCompleted
import kac.crawl.event.CrawlJobStarted
import kac.scraper.event.ScapeFinished
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.lang.Class


fun main(args: Array<String>) {

    val gson = Gson()

    val consulHost = System.getenv("CONSUL_HOST") ?: "http://localhost"
    val consulPort = System.getenv("CONSUL_PORT") ?: 8500
    val consulUri = "$consulHost:$consulPort"
    val localEnv = consulHost == "http://localhost"


    val client = HttpClient(Apache) {

        install(ConsulFeature) {
            consulUrl = consulUri
            local = localEnv
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

                    // Serialize the event back into
                    // it event type
                    val eventType = Class.forName(type)
                    val event = gson.fromJson(value, eventType)


                    println("New message on: $topic")

                    runBlocking {
                        when (eventType) {

                            // Result -> process new games against last run to determine newly opened games
                            ScapeFinished::class.java -> handler.handle(event as ScapeFinished)

                            // Result -> currently no-op, but could be used for reporting, etc
                            CrawlJobCompleted::class.java -> handler.handle(event as CrawlJobCompleted)

                            // Result -> start the scrape job to look for new games
                            CrawlJobStarted::class.java -> handler.handle(event as CrawlJobStarted)
                        }
                    }
                }
                catch (typeInvalidError: ClassNotFoundException) {}
                catch (typeInitError: ExceptionInInitializerError) {}
                catch (typeLinkageError: LinkageError) {}
                catch (badResponseError: BadResponseStatusException) {
                    // retry
                }

            }

        }

    }

}

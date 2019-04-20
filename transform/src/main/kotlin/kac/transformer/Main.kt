package kac.transformer

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
import kac.scraper.event.ScapeFinished
import kac.transformer.repository.TransformerRepository
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo


fun main(args: Array<String>) {

    val consulHost = System.getenv("CONSUL_HOST") ?: "http://localhost"
    val consulPort = System.getenv("CONSUL_PORT") ?: 8500
    val consulUri = "$consulHost:$consulPort"
    val localEnv = consulHost == "http://localhost"

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


    val properties = BrokerApplicationConfig().getProperties()
    val kafkaHosts = System.getenv("BROKER_BOOTSTRAP_SERVERS")
        ?: properties.getProperty("bootstrap.servers")
        ?: ""
    val kafkaTopics = properties.getProperty("topics").split(',')
    val groupId = properties.getProperty("groupId")

    val broker = KafkaDSL(kafkaHosts)
    val handler = Handler(client, newGamesRepository, broker, properties)

    // Listen for ScapeFinished Event from the scraper service
    broker.consumer(kafkaTopics, groupId) {

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
                        ScapeFinished::class.java -> handler.handle(event as ScapeFinished)
                    }
                }
            }
            catch (typeInvalidError: ClassNotFoundException) {}
            catch (typeInitError: ExceptionInInitializerError) {}
            catch (typeLinkageError: LinkageError) {}

        }

    }

}
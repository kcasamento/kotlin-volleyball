package kac.broker.dsl

import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.lang.reflect.Type
import java.time.Duration
import java.util.*
import javax.xml.bind.JAXBElement

class Consumer(
    kafkaConfig: KafkaConfig,
    topics: List<String>,
    groupId: String
) {

    private val kafkaConsumer: KafkaConsumer<String, String>
    private val gson: Gson

    @Volatile
    var keepGoing: Boolean = true

    init {

        val config = Properties()
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaConfig.bootstrapServers
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

        // TODO: Other Kafka consumer properties

        kafkaConsumer = KafkaConsumer<String, String>(config).apply {
            subscribe(topics)
        }

        gson = Gson()

    }

    fun consume(handler: (topic: String, key: String, value: String, valueType: String) -> Unit) = Thread(Runnable {

        keepGoing = true

        while(keepGoing) {
            kafkaConsumer.poll(Duration.ofSeconds(1))?.forEach {

                val typeHeader = it.headers().find { i -> i?.key() == "X-Payload-Type" }
                val typeName = String(typeHeader?.value()?: byteArrayOf())

                val topic = it?.topic() ?: ""
                val key = it?.key() ?: ""
                val value = it?.value() ?: ""

                handler(topic, key, value, typeName)

                kafkaConsumer.commitAsync()

            }
        }

    }).start()

    fun stop() {

        keepGoing = false

    }

}
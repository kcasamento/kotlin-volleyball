package kac.broker.dsl

import com.google.gson.Gson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.Future


class Producer(
    kafkaConfig: KafkaConfig,
    private val topic: String
) {

    private val kafkaProducer: KafkaProducer<String, String>
    private val gson: Gson

    init {

        val config = Properties()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaConfig.bootstrapServers
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer()::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer()::class.java

        // TODO: Other Kafka producer props

        kafkaProducer = KafkaProducer(config)
        gson = Gson()

    }

    fun <T:Any> send(message: T): Future<RecordMetadata> {

        val messageString = this.gson.toJson(message)

        val record = ProducerRecord<String, String>(topic, messageString)
        record.headers().add("X-Payload-Type", message::class.qualifiedName?.toByteArray())

        return kafkaProducer.send(record)

    }

    fun flush() = kafkaProducer.flush()
}
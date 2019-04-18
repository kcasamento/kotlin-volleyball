package kac.broker.dsl

class KafkaDSL(bootstrapServers: String) {

    private val kafkaConfig = KafkaConfig(bootstrapServers)

    fun producer(
        topic: String,
        doProduce: Producer.() -> Unit) =
        Producer(
            kafkaConfig,
            topic).doProduce()

    suspend fun <TReturn> producerAsync(
        topic: String,
        doProduce: suspend Producer.() -> TReturn) = Producer(
            kafkaConfig,
            topic).doProduce()


    fun consumer(
        topics: List<String>,
        groupId: String,
        doConsumer: Consumer.() -> Unit) =
        Consumer(
            kafkaConfig,
            topics,
            groupId).doConsumer()

    suspend fun consumerAsync(
        topics: List<String>,
        groupId: String,
        doConsumer: suspend Consumer.() -> Unit) =
        Consumer(
            kafkaConfig,
            topics,
            groupId).doConsumer()

}

fun kafka(bootstrapServers: String, init: KafkaDSL.() -> Unit) =
    KafkaDSL(bootstrapServers).init()
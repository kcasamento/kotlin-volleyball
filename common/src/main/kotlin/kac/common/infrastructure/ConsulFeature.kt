package kac.common.infrastructure

import com.orbitz.consul.Consul
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey

class ConsulFeature(var consulUrl: String, var local: Boolean) {

    class Config {
        var consulUrl: String = "http://localhost:8500"
        var local: Boolean = false

        fun build(): ConsulFeature = ConsulFeature(consulUrl, local)
    }

    companion object Feature : HttpClientFeature<Config, ConsulFeature> {

        var currentNodeIndex: Int = 0

        override val key = AttributeKey<ConsulFeature>("ConsulFeature")

        override fun prepare(block: Config.() -> Unit): ConsulFeature = Config().apply(block).build()

        override fun install(feature: ConsulFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) {

                val consulClient = Consul.builder().withUrl(feature.consulUrl).build()
                val nodes = consulClient.healthClient().getHealthyServiceInstances(context.url.host).response
                val selectedNode = nodes[currentNodeIndex]
                context.url.host = if(feature.local) "localhost" else selectedNode.service.address
                context.url.port = selectedNode.service.port
                currentNodeIndex = (currentNodeIndex + 1) % nodes.size
                println("Calling ${selectedNode.service.id}: ${context.url.buildString()}")

            }
        }

    }

}
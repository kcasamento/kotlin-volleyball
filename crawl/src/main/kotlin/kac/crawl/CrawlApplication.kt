package kac.crawl

import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

private const val ttl = 5L

fun main(args: Array<String>) {

    val server = embeddedServer(Netty, commandLineEnvironment(args))
    val serviceId = "crawl-${server.environment.connectors[0].port}"

    val consulHost = System.getenv("CONSUL_HOST") ?: "http://localhost"
    val consulPort = System.getenv("CONSUL_PORT") ?: 8500
    val serviceHost = System.getenv("CRAWL_HOST") ?: "localhost"

    val consulClient = Consul.builder().withUrl("$consulHost:$consulPort").build()
    val service = ImmutableRegistration.builder()
        .id(serviceId)
        .name("crawl-service")
        .address(serviceHost)
        .port(server.environment.connectors[0].port)
        .addChecks(Registration.RegCheck.ttl(ttl))
        .build()
    consulClient.agentClient().register(service)

    Timer("health-check", true).scheduleAtFixedRate(0, (ttl - (ttl * 0.1)).toLong()) {
        // Ping Consul heath check before TTL expires
        //println("$serviceId health check ping...")
        consulClient.agentClient().pass(serviceId)
    }

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Deregistering $serviceId from Consul...")
            consulClient.agentClient().deregister(serviceId)
        }
    })

    println("Starting $serviceId...")
    server.start(wait = true)

    println("$serviceId Terminating ...")

}


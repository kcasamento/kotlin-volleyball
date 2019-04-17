package kac.common.config

import java.io.IOException
import java.util.*


class BrokerApplicationConfig {

    private val configFileName: String = "broker.properties"

    fun getProperties(): Properties {
        val properties = Properties()
        try {
            properties.load(this.javaClass.classLoader.getResourceAsStream(configFileName))
            return properties
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return properties
    }

}
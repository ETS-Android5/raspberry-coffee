package restkt

import http.client.HTTPClient
import org.json.JSONObject

object KtMagReader {

    data class MagData(val heading: Double, val pitch: Double, val roll: Double)

    fun calculate(magX: Double, magY: Double, magZ: Double): MagData {
        var heading = Math.toDegrees(Math.atan2(magY, magX))
        while (heading < 0)
            heading += 360f
        val pitch = Math.toDegrees(Math.atan2(magY, magZ))
        val roll = Math.toDegrees(Math.atan2(magX, magZ))
        return MagData(heading, pitch, roll)
    }
}

fun main(args: Array<String>) {
    var keepLooping = true
    val restUrl = System.getProperty("rest.url", "http://192.168.42.9:8080/lis3mdl/cache")

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Gracefully shutting down...")
            keepLooping = false
            println("Gracefully shut !")
        }
    })

    while (keepLooping) {
        try {
            val str = HTTPClient.doGet(restUrl, null)
            if ("true" == System.getProperty("verbose"))
                println(str)
            val magData = JSONObject(str)
            val magX = magData.getDouble("x")
            val magY = magData.getDouble("y")
            val magZ = magData.getDouble("z")
            val data = KtMagReader.calculate(magX, magY, magZ)
            println("Heading:${data.heading}, Pitch:${data.pitch}, Roll:${data.roll}")
        } catch (ex: Exception) {
            println(ex.toString())
        }
    }
}

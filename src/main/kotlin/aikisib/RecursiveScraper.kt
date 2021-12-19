package aikisib

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import mu.KLogging
import java.io.File
import java.net.URL

interface RecursiveScraper {

    /**
     * Рекурсивно обойти сайт и сохранить в виде статических файлов.
     * Эквивалент `wget -mEpnp <нужный сайт>`
     * @param fromSite - какой сайт сохранить
     * @param toDir - куда сохранять
     */
    fun mirror(fromSite: URL, toDir: File)
}

internal class RecursiveScraperImpl : RecursiveScraper {

    private val client = HttpClient(CIO) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    logger().trace { message }
                }
            }
            level = LogLevel.INFO
        }
    }

    override fun mirror(fromSite: URL, toDir: File) {
        TODO("Not yet implemented")
    }

    companion object : KLogging()
}

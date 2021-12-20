package aikisib.mirror

import aikisib.url.UrlCanonicolizer
import aikisib.url.UrlCanonicolizerImpl
import mu.KLogging
import java.io.File
import java.net.URI
import java.net.URL

interface RecursiveScraper {

    /**
     * Рекурсивно обойти сайт и сохранить в виде статических файлов.
     * Эквивалент `wget -mEpnp <нужный сайт>`
     * @param fromSite - какой сайт сохранить
     * @param toDir - куда сохранять
     */
    suspend fun mirror(fromSite: URL, toDir: File)
}

internal class RecursiveScraperImpl : RecursiveScraper {

    private val downloader: Downloader = DownloaderImpl()
    private val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl

    override suspend fun mirror(fromSite: URL, toDir: File) {
        val rootUri = canonicolizer.canonicalize(URI("."), fromSite.toString())
        val rootPage = downloader.download(rootUri)
        println("Root page = $rootPage")
    }

    companion object : KLogging()
}

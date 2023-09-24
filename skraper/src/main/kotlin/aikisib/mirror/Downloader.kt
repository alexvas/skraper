package aikisib.mirror

import aikisib.model.OriginalDescription
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.fileExtensions
import io.ktor.http.lastModified
import io.ktor.http.takeFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KLogging
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes

interface Downloader {

    /**
     * Скачиваем локально ресурс по указанной ссылке
     * и сохраняем во временный файл.
     *
     * @param from - откуда качать.
     * @return описание скачанного ресурса, включая ссылку на файл.
     */
    suspend fun download(from: URI): OriginalDescription?
}

internal class DownloaderImpl(
    private val tempPath: Path,
    private val ignoredContentTypes: Set<ContentType>,
) : Downloader {

    private val client = HttpClient(Apache) {
        engine {
            socketTimeout = 10_000
            connectTimeout = 10_000
            connectionRequestTimeout = 20_000
        }
        expectSuccess = false
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    logger().trace { message }
                }
            }
            level = LogLevel.INFO
        }
    }

    @Suppress("ReturnCount")
    override suspend fun download(from: URI): OriginalDescription? {
        val response = client.get {
            url {
                takeFrom(from)
            }
        }
        if (response.status != HttpStatusCode.OK) {
            logger.warn { "Статус ${response.status} для $from." }
            return null
        }
        val contentType = response.contentType()
        check(contentType != null) { "Скачали не пойми что: $from" }
        if (contentType.withoutParameters() in ignoredContentTypes) {
            return null
        }
        val hostPrefix = from.host.replace('.', '_')
        val suffix = contentType.extension()

        val output = withContext(Dispatchers.IO) {
            val outputContent = createTempFile(tempPath, hostPrefix, ".$suffix")
            val buffer = response.readBytes()
            outputContent.writeBytes(buffer)
            outputContent to buffer.size
        }

        return OriginalDescription(
            remoteUri = from,
            type = contentType.withoutParameters(),
            size = output.second / BYTES_IN_KILOBYTE,
            localPath = output.first,
            lastModified = response.lastModified(),
        )
    }

    private fun ContentType.extension(): String =
        when (this.withoutParameters()) {
            ContentType.Text.Html -> "html"
            ContentType.Image.JPEG -> "jpg"
            else -> this.fileExtensions().firstOrNull() ?: "unknown"
        }

    companion object : KLogging() {
        private const val BYTES_IN_KILOBYTE = 1024
    }
}

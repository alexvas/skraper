package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.UrlCanonicolizer
import mu.KLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import kotlin.io.path.readText

interface LinkExtractor {

    /**
     * Извлекает ссылки из оригинального документа.
     *
     * @param originalDescription
     * @return отображение текстовых ссылок на разрешённые в канонические
     */
    fun extractLinks(originalDescription: OriginalDescription): Map<String, URI>
}

internal class LinkExtractorImpl(
    private val rootUri: URI,
    private val uriCanonicolizer: UrlCanonicolizer,
) : LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val doc: Document = Jsoup.parse(from.readText())
        doc.getElementsByTag("a").forEach {
            val href = it.attr("href")
            val canonical = try {
                uriCanonicolizer.canonicalize(rootUri, href)
            } catch (e: IllegalArgumentException) {
                logger.warn { "IAE для ссылки '$href' на страничке $from" }
                null
            } catch (e: NullPointerException) {
                logger.warn { "NPE для ссылки '$href' на страничке $from" }
                null
            }
            if (canonical != null)
                result[href] = canonical
        }

        return result
    }

    companion object : KLogging()
}

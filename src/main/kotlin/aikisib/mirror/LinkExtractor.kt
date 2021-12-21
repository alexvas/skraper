package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.UrlCanonicolizer
import io.ktor.http.ContentType
import mu.KLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.nio.file.Path
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
    rootUri: URI,
    uriCanonicolizer: UrlCanonicolizer,
) : LinkExtractor {

    private val delegates = mapOf<ContentType, LinkExtractor>(
        ContentType.Text.Html to HtmlLinkExtractor(rootUri, uriCanonicolizer),
    )

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val delegate = delegates[originalDescription.type] ?: return emptyMap()
        return delegate.extractLinks(originalDescription)
    }

    companion object : KLogging()
}

private class HtmlLinkExtractor(
    private val rootUri: URI,
    private val uriCanonicolizer: UrlCanonicolizer,
) : LinkExtractor {

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val doc: Document = Jsoup.parse(from.readText())
        doc.getElementsByTag("a").forEach {
            val href = it.attr("href")
            result.maybeAdd(href, from)
        }
        doc.getElementsByTag("link").forEach {
            val href = it.attr("href")
            result.maybeAdd(href, from)
        }

        return result
    }

    private fun MutableMap<String, URI>.maybeAdd(href: String, from: Path) {
        if (href.isEmpty())
            return
        val canonical = try {
            uriCanonicolizer.canonicalize(rootUri, href)
        } catch (e: IllegalArgumentException) {
            logger.warn { "IAE для ссылки '$href' на страничке $from" }
            null
        } catch (e: NullPointerException) {
            logger.warn { "NPE для ссылки '$href' на страничке $from" }
            null
        } ?: return
        this[href] = canonical
    }

    companion object : KLogging()
}

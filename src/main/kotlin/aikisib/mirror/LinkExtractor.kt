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

internal class LinkExtractorImpl(uriCanonicolizer: UrlCanonicolizer) : LinkExtractor {

    private val delegates: Map<ContentType, LinkExtractor> = mapOf(
        ContentType.Text.Html to HtmlLinkExtractor(uriCanonicolizer),
        ContentType.Text.CSS to CssLinkExtractor(uriCanonicolizer),
    )

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val delegate = delegates[originalDescription.type] ?: return emptyMap()
        return delegate.extractLinks(originalDescription)
    }

    companion object : KLogging()
}

private abstract class LinkExtractorBase(
    private val uriCanonicolizer: UrlCanonicolizer,
) {

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    fun MutableMap<String, URI>.maybeAdd(pageUri: URI, href: String, from: Path) {
        if (href.isEmpty())
            return
        if (href.startsWith("data:"))
            return
        val canonical = try {
            uriCanonicolizer.canonicalize(pageUri, href)
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


private class HtmlLinkExtractor(uriCanonicolizer: UrlCanonicolizer) :
    LinkExtractorBase(uriCanonicolizer), LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val doc: Document = Jsoup.parse(from.readText())
        doc.getElementsByTag("a").forEach {
            val href = it.attr("href")
            result.maybeAdd(originalDescription.remoteUri, href, from)
        }
        doc.getElementsByTag("link").forEach {
            val href = it.attr("href")
            result.maybeAdd(originalDescription.remoteUri, href, from)
        }
        doc.getElementsByTag("script").forEach {
            val src = it.attr("src")
            result.maybeAdd(originalDescription.remoteUri, src, from)
        }

        return result
    }
}

private class CssLinkExtractor(uriCanonicolizer: UrlCanonicolizer) :
    LinkExtractorBase(uriCanonicolizer), LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val text = from.readText()
        for (m in urlRegex.findAll(text)) {
            val link = m.groupValues.asSequence()
                .drop(1)
                .filter { it.isNotBlank() }
                .firstOrNull() ?: continue
            result.maybeAdd(originalDescription.remoteUri, link, from)
        }
        return result
    }

    companion object {
        private val urlRegex = Regex("""url\((?:'([^']++)'|"([^"]++)"|([^)]++))\)""")
    }

}

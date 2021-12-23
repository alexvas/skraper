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
    uriCanonicolizer: UrlCanonicolizer,
    forbiddenPrefixes: Set<String>,
) : LinkExtractor {

    private val delegates: Map<ContentType, LinkExtractor> = mapOf(
        ContentType.Text.Html to HtmlLinkExtractor(uriCanonicolizer, forbiddenPrefixes),
        ContentType.Text.CSS to CssLinkExtractor(uriCanonicolizer, forbiddenPrefixes),
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
    forbiddenPrefixes: Set<String>,
) {

    private val ignoredPrefixes = forbiddenPrefixes + IGNORED_PROTOCOL_PREFIXES

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    fun MutableMap<String, URI>.maybeAdd(pageUri: URI, href: String, from: Path) {
        if (href.isEmpty())
            return
        if (ignoredPrefixes.any { prefix -> href.startsWith(prefix) })
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

    fun extractLinkFromCssStyle(
        cssStyle: String?,
        result: MutableMap<String, URI>,
        remoteUri: URI,
        from: Path,
    ) {
        if (cssStyle.isNullOrBlank())
            return
        for (m in urlRegex.findAll(cssStyle)) {
            val link = m.groupValues.asSequence()
                .drop(1)
                .filter { it.isNotBlank() }
                .firstOrNull() ?: continue
            result.maybeAdd(remoteUri, link, from)
        }
    }

    companion object : KLogging() {
        private val IGNORED_PROTOCOL_PREFIXES = setOf(
            "data:", // встроенные данные
            "tel:", // номер телефона
            "javascript:", // встроенный javascript
        )
        private val urlRegex = Regex("""url\((?:'([^']++)'|"([^"]++)"|([^)]++))\)""")
    }
}

private class HtmlLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    forbiddenPrefixes: Set<String>,
) : LinkExtractorBase(uriCanonicolizer, forbiddenPrefixes), LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val remoteUri = originalDescription.remoteUri
        val from = originalDescription.localPath
        val doc: Document = Jsoup.parse(from.readText())
        doc.getElementsByTag("a").forEach {
            val href = it.attr("href")
            result.maybeAdd(remoteUri, href, from)
        }
        doc.getElementsByTag("link").forEach {
            val href = it.attr("href")
            result.maybeAdd(remoteUri, href, from)
        }
        doc.getElementsByTag("script").forEach {
            val src = it.attr("src")
            result.maybeAdd(remoteUri, src, from)
        }
        // вытаскиваем контент Slider revolution:
        doc.getElementsByTag("rs-slide").forEach {
            val dataThumb = it.attr("data-thumb")
            result.maybeAdd(remoteUri, dataThumb, from)
        }
        doc.getElementsByTag("img").forEach {
            val src = it.attr("src")
            result.maybeAdd(remoteUri, src, from)
            // вытаскиваем контент Slider revolution:
            val dataLazyload = it.attr("data-lazyload")
            result.maybeAdd(remoteUri, dataLazyload, from)
        }
        doc.getElementsByTag("video").forEach {
            val src = it.attr("src")
            result.maybeAdd(remoteUri, src, from)
        }
        // <meta name="msapplication-TileImage" content
        doc.getElementsByTag("meta")
            .filter { it.attr("name") == "msapplication-TileImage" }
            .forEach {
                val content = it.attr("content")
                result.maybeAdd(remoteUri, content, from)
            }
        // <div class="sc_parallax_content" style="background-image:url(https://aikisib.ru/w
        doc.allElements
            .forEach {
                val cssStyle = it.attr("style")
                extractLinkFromCssStyle(cssStyle, result, remoteUri, from)
            }

        return result
    }
}

private class CssLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    forbiddenPrefixes: Set<String>,
) : LinkExtractorBase(uriCanonicolizer, forbiddenPrefixes), LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val remoteUri = originalDescription.remoteUri
        val from = originalDescription.localPath
        val cssStyle = from.readText()
        extractLinkFromCssStyle(cssStyle, result, remoteUri, from)
        return result
    }
}

/**
 * Из Json можно (при желании) кучу мусора вытащить наружу.
 * Всё работает без него. Комментирую этот класс.
 */
@Suppress("unused")
private class JsonLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    forbiddenPrefixes: Set<String>,
    rootUri: URI,
) : LinkExtractorBase(uriCanonicolizer, forbiddenPrefixes), LinkExtractor {

    private val rootUriPrefixRegex: Regex

    init {
        val rootUriPrefix = rootUri.toString()
            .removeSuffix("/")
            .replace("/", "\\/")
        rootUriPrefixRegex = Regex("""(\Q${rootUriPrefix}\E[^")<' ]++)""")
    }

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val text = from.readText()
        for (m in rootUriPrefixRegex.findAll(text)) {
            val link = m.groupValues[1].replace("\\/", "/")
                .removeSuffix("\\")
            result.maybeAdd(originalDescription.remoteUri, link, from)
        }
        return result
    }
}

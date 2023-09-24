package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.UrlCanonicolizer
import io.ktor.http.ContentType
import mu.KLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeFilter.FilterResult.CONTINUE
import org.jsoup.select.NodeFilter.FilterResult.SKIP_ENTIRELY
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
    ignoredPrefixes: Set<String>,
    ignoredSuffixes: Set<String>,
) : LinkExtractor {

    private val delegates: Map<ContentType, LinkExtractor> = mapOf(
        ContentType.Text.Html to HtmlLinkExtractor(uriCanonicolizer, ignoredPrefixes, ignoredSuffixes),
        ContentType.Text.CSS to CssLinkExtractor(uriCanonicolizer, ignoredPrefixes, ignoredSuffixes),
    )

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // исключения здесь обрабатываются адекватно
    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val delegate = delegates[originalDescription.type] ?: return emptyMap()
        return delegate.extractLinks(originalDescription)
    }

    companion object : KLogging()
}

@Suppress("UnnecessaryAbstractClass")
private abstract class LinkExtractorBase(
    private val uriCanonicolizer: UrlCanonicolizer,
    ignoredPrefixes: Set<String>,
    ignoredSuffixes: Set<String>,
) {

    private val ignoredPrefixes = ignoredPrefixes + IGNORED_PROTOCOL_PREFIXES
    private val ignoredSuffixes = ignoredSuffixes.map {
        // гарантируем, что расширение начинается с точки
        "." + it.removePrefix(".")
    }

    // исключения здесь обрабатываются адекватно
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    fun MutableMap<String, URI>.maybeAdd(pageUri: URI, href: String, from: Path) {
        if (href.isEmpty()) {
            return
        }
        if (ignoredPrefixes.any { prefix -> href.startsWith(prefix) }) {
            return
        }
        if (ignoredSuffixes.any { suffix -> href.endsWith(suffix) }) {
            return
        }
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

    companion object : KLogging() {
        private val IGNORED_PROTOCOL_PREFIXES = setOf(
            "data:", // встроенные данные
            "tel:", // номер телефона
            "javascript:", // встроенный javascript
        )
    }
}

private class HtmlLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    ignoredPrefixes: Set<String>,
    ignoredSuffixes: Set<String>,
) : LinkExtractorBase(uriCanonicolizer, ignoredPrefixes, ignoredSuffixes), LinkExtractor {

    @Suppress("LongMethod")
    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val remoteUri = originalDescription.remoteUri
        val from = originalDescription.localPath
        val doc: Document = Jsoup.parse(from.readText())
        doc.getElementsByTag("a").forEach {
            val href = it.attr("href")
            result.maybeAdd(remoteUri, href, from)
            val dataThumb = it.attr("data-thumb")
            result.maybeAdd(remoteUri, dataThumb, from)
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
            // вытаскиваем контент странички "о нас"
            val srcset = it.attr("srcset")
            if (!srcset.isNullOrBlank()) {
                srcset.splitToSequence(',')
                    .map { input ->
                        input.trim().split(' ')[0]
                    }
                    .forEach { input ->
                        result.maybeAdd(remoteUri, input, from)
                    }
            }
        }
        doc.getElementsByTag("video").forEach {
            val src = it.attr("src")
            result.maybeAdd(remoteUri, src, from)
        }

        // <meta name="msapplication-TileImage" content
        val onlyMsApplicationTileImage = NodeFilter { node, _ ->
            if (node.attr("name") == "msapplication-TileImage") {
                CONTINUE
            } else {
                SKIP_ENTIRELY
            }
        }
        doc.getElementsByTag("meta")
            .filter(onlyMsApplicationTileImage)
            .forEach {
                val content = it.attr("content")
                result.maybeAdd(remoteUri, content, from)
            }
        // <div class="sc_parallax_content" style="background-image:url(https://aikisib.ru/w
        doc.allElements
            .forEach {
                val cssStyle = it.attr("style")
                cssStyle.extractLinks().forEach { link ->
                    result.maybeAdd(remoteUri, link, from)
                }
            }

        return result
    }
}

private class CssLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    ignoredPrefixes: Set<String>,
    ignoredSuffixes: Set<String>,
) : LinkExtractorBase(uriCanonicolizer, ignoredPrefixes, ignoredSuffixes), LinkExtractor {

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val remoteUri = originalDescription.remoteUri
        val from = originalDescription.localPath
        val cssStyle = from.readText()
        cssStyle.extractLinks().forEach { link ->
            result.maybeAdd(remoteUri, link, from)
        }
        return result
    }
}

internal fun String?.extractLinks() =
    if (isNullOrBlank()) {
        sequenceOf()
    } else {
        urlRegex.findAll(this)
            .map { m ->
                m.groupValues.asSequence()
                    .drop(1)
                    .filter { it.isNotBlank() }
                    .firstOrNull()
            }
            .filterNotNull()
    }

private val urlRegex = Regex("""url\((?:'([^']++)'|"([^"]++)"|([^)]++))\)""")

/**
 * Из Json можно (при желании) кучу мусора вытащить наружу.
 * Всё работает без него. Комментирую этот класс.
 */
@Suppress("unused")
private class JsonLinkExtractor(
    uriCanonicolizer: UrlCanonicolizer,
    ignoredPrefixes: Set<String>,
    ignoredSuffixes: Set<String>,
    rootMain: URI,
) : LinkExtractorBase(uriCanonicolizer, ignoredPrefixes, ignoredSuffixes), LinkExtractor {

    private val rootMainPrefixRegex: Regex

    init {
        val rootMainPrefix = rootMain.toString()
            .removeSuffix("/")
            .replace("/", "\\/")
        rootMainPrefixRegex = Regex("""(\Q$rootMainPrefix\E[^")<' ]++)""")
    }

    override fun extractLinks(originalDescription: OriginalDescription): Map<String, URI> {
        val result = mutableMapOf<String, URI>()
        val from = originalDescription.localPath
        val text = from.readText()
        for (m in rootMainPrefixRegex.findAll(text)) {
            val link = m.groupValues[1].replace("\\/", "/")
                .removeSuffix("\\")
            result.maybeAdd(originalDescription.remoteUri, link, from)
        }
        return result
    }
}

package aikisib.url

import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import java.net.URI
import java.net.URLDecoder
import kotlin.text.Charsets.UTF_8

/**
 * Сервис для трансляции URL к форме, удобной для локального сохранения.
 */
interface UrlTransformer {

    /**
     * Добавляем расширение .html страничкам.
     * Добавляем расширение .css стилям.
     * Добавляем query-параметры в путь.
     *
     * @param contentType тип содержимого
     * @param input входная ссылка
     * @return результирующий URI, возможно невалидный.
     */
    fun transform(contentType: ContentType, input: URI): URI
}

@Suppress("TooManyFunctions")
internal object UrlTransformerImpl : UrlTransformer {
    private val extensions = mapOf(
        ContentType.Text.Html to "html",
        ContentType.Text.CSS to "css",
        ContentType.Text.JavaScript to "js",
        ContentType.Text.Xml to "xml",
        ContentType.Application.Json to "json",
        ContentType.Application.JavaScript to "js",
        ContentType.Application.Rss to "xml",
        ContentType.Image.JPEG to "jpg",
        ContentType.Image.PNG to "png",
        ContentType.Image.SVG to "svg",
    )

    override fun transform(contentType: ContentType, input: URI): URI {
        val query = input.query
        if (query.isNullOrBlank())
            return input.maybeFixExtension(contentType)

        val shouldBeExt = extensions[contentType]
            ?: error("не определено расширение для URI $input с типом содержимого $contentType")
        val p = input.rawPath
        val last = p.splitToSequence('/').last()
        return when (last.substringAfterLast('.')) {
            last -> {
                require(contentType == ContentType.Text.Html) {
                    "Непонятный путь, который не содержит расширений: '$p' с типом содержимого $contentType"
                }
                input.addHtmlPathSegment()
            }
            else -> input.appendQueryAndExtension(shouldBeExt)
        }
    }

    private fun URI.addHtmlPathSegment(): URI {
        val segmentToAdd = "?$query".fsEncode() + ".html"
        val pathSegments = when (rawPath) {
            "/" -> SINGLE_EMPTY_SEGMENT_LIST
            else -> rawPath.split('/')
                .map { it.reEncode() }
        }
        return withPathSegmentsAndNoQuery(pathSegments + segmentToAdd)
    }

    private fun URI.appendQueryAndExtension(shouldBeExt: String): URI {
        val inputSegments = rawPath.split('/').map { it.reEncode() }
        val last = inputSegments.last()
        val pathSegmentsWoLast: List<String> = inputSegments.toMutableList().also { it.removeLast() }
        val segmentToBeLast = last + "?$query".fsEncode() + ".$shouldBeExt"
        val pathSegments = pathSegmentsWoLast + segmentToBeLast
        return withPathSegmentsAndNoQuery(pathSegments)
    }

    private fun URI.withPathAndNoQuery(path: String): URI {
        val inputSegments = path.split('/').map { it.reEncode() }
        val last = inputSegments.last()
        val pathSegmentsWoLast: List<String> = inputSegments.toMutableList().also { it.removeLast() }
        val filename = last.substringBeforeLast('.')
        val ext = last.substringAfterLast('.')
        val segmentToBeLast = "$filename.$ext"
        val pathSegments = pathSegmentsWoLast + segmentToBeLast
        return withPathSegmentsAndNoQuery(pathSegments)
    }

    private fun URI.withPathSegmentsAndNoQuery(pathSegments: List<String>): URI {
        return URLBuilder().takeFrom(this)
            .also { urlBuilder ->
                urlBuilder.encodedPathSegments = pathSegments
                urlBuilder.encodedParameters.clear()
            }
            .build()
            .toURI()
    }

    private fun URI.maybeFixExtension(contentType: ContentType): URI {
        val shouldBeExt = extensions[contentType] ?: return this
        require(!rawPath.isNullOrBlank()) { "канонический URI должен иметь ненулевой путь, а не $this" }

        val fixedPath = rawPath.maybeFixExtension(contentType, shouldBeExt)
        return withPathAndNoQuery(fixedPath)
    }

    private fun String.maybeFixExtension(contentType: ContentType, shouldBeExt: String): String {
        if (this == "/")
            return "/index.html"
        val last = splitToSequence('/').last()
        return when (last.substringAfterLast('.')) {
            shouldBeExt -> this
            last -> addExtension(contentType)
            else -> "$this.$shouldBeExt"
        }
    }

    private fun String.addExtension(contentType: ContentType): String {
        return when(contentType) {
            ContentType.Text.Html -> "$this/index.html"
            ContentType.Application.Json -> "$this/index.json"
            ContentType.Application.Rss -> "$this/index.xml"
            else -> error("Непонятный путь, который не содержит расширений: '$this' для типа $contentType")
        }
    }

    /**
     * Браузеры скептически относятся к URL-кодированным символам,
     * поэтому заменяем символ процента на символ подчерка.
     */
    private fun String.fsEncode() =
        encode()
            .replace('%', '_')

    private fun String.reEncode() =
        if (this.isEmpty())
            this
        else
            URLDecoder.decode(this, UTF_8).encode()

    private fun String.encode() =
        FsEncoder.encode(this)

    private val SINGLE_EMPTY_SEGMENT_LIST = listOf("")
}

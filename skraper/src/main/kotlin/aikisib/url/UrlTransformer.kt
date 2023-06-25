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
    private val VND_MS_FONTOBJECT = ContentType(ContentType.Application.Any.contentType, "vnd.ms-fontobject")
    private val VIDEO_WEBM = ContentType(ContentType.Video.Any.contentType, "webm")
    private val FONT_WOFF = ContentType("font", "woff")
    private val FONT_WOFF2 = ContentType("font", "woff2")

    private val extensions: Map<ContentType, String> = mapOf(
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
        VND_MS_FONTOBJECT to "eot",
        VIDEO_WEBM to "webm",
        FONT_WOFF to "woff",
        FONT_WOFF2 to "woff2",
    )

    override fun transform(contentType: ContentType, input: URI): URI {
        val query = input.query
        if (query.isNullOrBlank()) {
            return input.maybeFixExtension(contentType)
        }

        val p = input.rawPath
        val shouldBeExt = findExtension(contentType, p)
        val last = p.splitToSequence('/').last()
        return when (last.substringAfterLast('.')) {
            last -> input.addHtmlPathSegment(shouldBeExt)
            else -> input.appendQueryAndExtension(shouldBeExt)
        }
    }

    private fun findExtension(contentType: ContentType, input: String): String {
        val ext = extensions[contentType]
        if (ext != null) {
            return ext
        }
        return when (contentType) {
            ContentType.Application.OctetStream -> findExtensionForOctetStream(input)
            else -> error("не определено расширение для пути $input с типом содержимого $contentType")
        }
    }

    private fun findExtensionForOctetStream(input: String) =
        when {
            input.endsWith(".woff2") -> "woff2"
            input.endsWith(".ttf") -> "ttf"
            else -> error("не определено расширение для пути $input с типом содержимого поток октетов")
        }

    private fun URI.addHtmlPathSegment(ext: String): URI {
        val segmentToAdd = "?$query".fsEncode() + ".$ext"
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
        if (this == "/") {
            return "/index.html"
        }
        val last = splitToSequence('/').last()
        return when (last.substringAfterLast('.')) {
            shouldBeExt -> this
            last -> addExtension(contentType)
            else -> "$this.$shouldBeExt"
        }
    }

    private fun String.addExtension(contentType: ContentType) =
        this + "/index." + findExtension(contentType, this)

    /**
     * Браузеры скептически относятся к URL-кодированным символам,
     * поэтому заменяем символ процента на символ подчерка.
     */
    private fun String.fsEncode() =
        encode()
            .replace('%', '_')

    private fun String.reEncode() =
        if (this.isEmpty()) {
            this
        } else {
            URLDecoder.decode(this, UTF_8).encode()
        }

    private fun String.encode() =
        FsEncoder.encode(this)

    private val SINGLE_EMPTY_SEGMENT_LIST = listOf("")
}

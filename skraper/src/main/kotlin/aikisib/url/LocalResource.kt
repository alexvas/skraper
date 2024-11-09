package aikisib.url

import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Ресурс для копирования в целевую статическую копию сайта.
 */
data class LocalResource(

    /**
     * Где ресурс расположен на исходном динамическом сайте.
     */
    val source: URI,

    /**
     * Что содержит ресурс.
     */
    val contentType: ContentType,

    /**
     * Нормализованное исходное расположение ресурса.
     * Используется как ключ для кэширования результатов трансформации.
     */
    val normalizedSource: URI,

    /**
     * Как сослаться на этот ресурс в целевой статической копии сайта.
     * Путь от корня сайта.
     */
    val reference: URI,

    /**
     * Куда в файловой системе сохранить ресурс для целевой статической копии сайта.
     */
    val target: Path,
) {
    companion object {

        /**
         * Из обычной HTML странички
         */
        fun fromHtmlPage(source: URI): LocalResource {
            val normalizedSource = source.norm()

            val path = source.path
            val query = source.query
            val fixedPath = maybeFixHtmlPath(path, query)
            val fragment = source.fragment

            val reference = createReference(fixedPath, fragment)
            val target = createHtmlTarget(fixedPath)

            return LocalResource(source, ContentType.Text.Html, normalizedSource, reference, target)
        }

        private fun createReference(fixedPath: String, fragment: String?): URI {
            val withLeadingSlash = when (fixedPath){
                "/" -> "/"
                else -> "/$fixedPath"
            }

            return URI(
                null,
                null,
                null,
                -1,
                withLeadingSlash,
                null,
                fragment,
            )
        }

        private fun maybeFixHtmlPath(path: String?, query: String?): String {
            return maybeFixHtmlPath(path + (query?.let { "?$it".fsEncode() } ?: ""))
        }

        private fun maybeFixHtmlPath(path: String?) =
            path?.lowercase()
                ?.removeSuffix("/index.html")
                ?.removeSuffix(".html")
                ?.removeSuffix("/")
                ?.removePrefix("/")
                ?.takeIf { it.isNotBlank() }
                ?.maybeEncode()
                ?: "/"

        private fun String.maybeEncode() = when {
            !containsSpecialCharacters() -> this
            contains('/') -> split('/').joinToString(separator = "/") { it.reEncode() }
            else -> reEncode()
        }

        private fun createHtmlTarget(fixedPath: String) =
            when (fixedPath) {
                "/" -> Path("index.html")
                else -> Path(fixedPath, "index.html")
            }

        /**
         * Из всего, кроме обычной HTML странички.
         */
        fun fromEtc(source: URI, contentType: ContentType): LocalResource {
            val normalizedSource = source.norm()

            val query = source.query
            val path = source.path
            require(path != null) {
                "No path for URL $source of content-type $contentType."
            }

            val rawPath = source.rawPath
            val shouldBeExt = findExtension(contentType, rawPath)

            val fragment = source.fragment
            val fixedPath = maybeFixEtcPath(path, shouldBeExt, query)
            val reference = createReference(fixedPath, fragment)
            val target = Path(fixedPath)
            return LocalResource(source, contentType, normalizedSource, reference, target)
        }

        private fun maybeFixEtcPath(path: String, shouldBeExt: String, query: String?): String {
            val fixedPath = maybeFixEtcPath(path, shouldBeExt)
            val effectivePath = fixedPath + (query?.let { "?$it".fsEncode() }?.lowercase() ?: "")
            return "$effectivePath.$shouldBeExt"
        }

        private fun maybeFixEtcPath(path: String, shouldBeExt: String) =
            path.lowercase()
                .removeSuffix(".$shouldBeExt")
                .removePrefix("/")
                .maybeEncode()

        /**
         * Приводим path и query к нижнему регистру и зануляем фрагмент.
         */
        private fun URI.norm() =
            URI(
                scheme,
                userInfo,
                host,
                port,
                path?.lowercase(),
                query?.lowercase(),
                /* зануляем фрагмент */
                null,
            )


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
                URLDecoder.decode(this, Charsets.UTF_8).encode()
            }

        private fun String.encode() =
            FsEncoder.encode(this)

        private val SPECIAL_CHAR = Regex("[\\\\?%*:|\"'<>,=;\\s]|[^\\p{Print}]")

        private fun String?.containsSpecialCharacters() =
            this != null && SPECIAL_CHAR.containsMatchIn(this)
    }
}


package aikisib.url

import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPath
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import java.net.URI

/**
 * Сервис для создания канонического адреса страницы.
 * <link rel="canonical" href="http://www.example.com/blog"/>
 */
interface UrlCanonicalizer {

    /**
     * В некоторых случаях (канонический адрес) таки требуется
     * ASCII-символьный URI. Вот способ его получить.
     */
    fun encode(target: URI): URI
}

internal object UrlCanonicalizerImpl : UrlCanonicalizer {

    override fun encode(target: URI) =
        target.encodePath()
}

private fun URI.encodePath(): URI {
    return URLBuilder().takeFrom(this)
        .also { urlBuilder ->
            val segments = urlBuilder.encodedPathSegments
            val encoded = segments.map { it.encodeURLPath() }
            urlBuilder.encodedPathSegments = encoded
        }
        .build()
        .toURI()
}

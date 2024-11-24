package aikisib.mirror

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodeURLPath
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import java.net.URI

/**
 * Фильтр ссылок. Делит ссылки на внутренние и внешние.
 */
interface FromLinkFilter {

    /**
     * Является ли входной URI внутренним для указанного корневого,
     * а также псевдонимов? При положительном ответе возвращает
     * ненулевую пару со скорректированным URI относительно
     * корневого.
     */
    fun filter(link: String, uri: URI): Pair<String, URI>?
}

internal class FromLinkFilterImpl(
    private val rootMain: URI,
    fromAliases: List<URI>,
) : FromLinkFilter {

    private val anyRootList = fromAliases + rootMain

    override fun filter(link: String, uri: URI): Pair<String, URI>? {
        anyRootList.forEach { anyRoot ->
            val inputSameScheme = uri.sameSchemeAs(anyRoot)
            val maybeRelative = anyRoot.relativize(inputSameScheme)
            if (maybeRelative != inputSameScheme) {
                return link to uri.deAlias()
            }
        }
        return null
    }

    // подменяем псевдоним на основной URI
    private fun URI.deAlias() =
        sameOrigin(rootMain)
}

internal fun URI.sameOrigin(ethalone: URI) =
    URI(
        ethalone.scheme,
        ethalone.userInfo,
        ethalone.host,
        ethalone.port,
        path,
        query,
        fragment,
    )

internal fun URI.rawSameOrigin(ethalone: URI): URI {
    return URLBuilder().takeFrom(this)
        .also { urlBuilder ->
            val segments = urlBuilder.encodedPathSegments
            val encoded = segments.map { it.encodeURLPath() }
            urlBuilder.encodedPathSegments = encoded
            urlBuilder.protocol = when(ethalone.scheme) {
                "http" -> URLProtocol.HTTP
                "https" -> URLProtocol.HTTPS
                else -> throw IllegalArgumentException("Illegal scheme of $ethalone")
            }
            urlBuilder.user = ethalone.userInfo
            urlBuilder.host = ethalone.host
            urlBuilder.port = if (ethalone.port in 0..65535) ethalone.port else 0
        }
        .build()
        .toURI()
}

private fun URI.sameSchemeAs(anyRoot: URI) =
    URI(
        anyRoot.scheme,
        userInfo,
        host,
        port,
        path,
        query,
        fragment,
    )

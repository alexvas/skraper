package aikisib.mirror

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
            if (maybeRelative != inputSameScheme)
                return link to uri.deAlias()
        }
        return null
    }

    // подменяем псевдоним на основной URI
    private fun URI.deAlias() = URI(
        rootMain.scheme,
        rootMain.userInfo,
        rootMain.host,
        rootMain.port,
        path,
        query,
        fragment,
    )
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

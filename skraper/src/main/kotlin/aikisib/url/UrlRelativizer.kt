package aikisib.url

import java.net.URI

/**
 * Сервис для построения относительных ссылок.
 */
interface UrlRelativizer {

    /**
     * Конструирует относительную ссылку для указанного местоположения.
     *
     * @param source - откуда ссылаются.
     * @param target - ссылка на элемент.
     */
    fun relativize(source: URI, target: LocalResource, rawFragment: String?): URI

    /**
     * Конструирует относительную ссылку для указанного местоположения.
     *
     * @param source - откуда ссылаются.
     * @param target - ссылка на элемент.
     */
    fun relativize(source: LocalResource, target: LocalResource, rawFragment: String?): URI

    /**
     * Конструирует относительную ссылку для указанного местоположения.
     *
     * @param source - откуда ссылаются.
     * @param target - ссылка на элемент.
     */
    fun relativize(source: LocalResource, target: LocalResource): URI
}

internal object UrlRelativizerImpl : UrlRelativizer {

    override fun relativize(source: URI, target: LocalResource, rawFragment: String?): URI {
        TODO("Not yet implemented")
    }

    override fun relativize(source: LocalResource, target: LocalResource, rawFragment: String?): URI {
        TODO("Not yet implemented")
    }

    override fun relativize(source: LocalResource, target: LocalResource): URI {
        TODO("Not yet implemented")
    }

    private fun URI.onlyFragment() = URI(
        /* scheme = */
        null,
        /* userInfo = */
        null,
        /* host = */
        null,
        /* port = */
        -1,
        /* path = */
        null,
        /* query = */
        null,
        /* fragment = */
        this.fragment,
    )

    private fun URI.onlyPathAndFragment(): URI {
        val uri = if (fragment.isNullOrBlank()) {
            rawPath
        } else {
            "$rawPath#$fragment"
        }
        return URI(uri)
    }
}

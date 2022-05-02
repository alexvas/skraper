package aikisib.url

import java.net.URI
import kotlin.math.min

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
    fun relativize(source: URI, target: URI, rawFragment: String?): URI
}

internal object UrlRelativizerImpl : UrlRelativizer {

    override fun relativize(source: URI, target: URI, rawFragment: String?): URI {
        require(source.isAbsolute) { "URL источника должен быть абсолютным, а не $source" }
        require(target.isAbsolute) { "Целевой URL должен быть абсолютным, а не $target" }
        require(source.host == target.host) { "Хосты различаются. Source: ${source.host}, Target: ${target.host}" }
        require(source.port == target.port) { "Порты различаются. Source: ${source.port}, Target: ${target.port}" }

        val sourcePathSegments = source.rawPath.split('/')
        val targetPathSegments = target.rawPath.split('/')

        // пропускаем общие элементы пути
        var index = 0
        val maxIndex = min(sourcePathSegments.size, targetPathSegments.size)
        while (index < maxIndex && sourcePathSegments[index] == targetPathSegments[index]) {
            ++index
        }

        if (index == maxIndex) {
            // Та же самая страничка
            return relativeUri(target.query, target.fragment)
        }

        // Сколько раз надо подняться наверх. Один из сегментов пути -- это имя файла.
        val upDirCount = sourcePathSegments.size - index - 1
        val resultChunks = MutableList(upDirCount) { ".." }
        while (index < targetPathSegments.size) {
            resultChunks.add(targetPathSegments[index])
            ++index
        }

        return relativeUri(resultChunks, target.query, rawFragment)
    }

    private fun relativeUri(
        resultChunks: MutableList<String>,
        query: String?,
        fragment: String?,
    ): URI {
        val path = resultChunks.joinToString(separator = "/")
        val pathWithQuery = if (query.isNullOrBlank())
            path
        else
            "$path?$query"
        val pathWithQueryAndFragment = if (fragment.isNullOrBlank())
            pathWithQuery
        else
            "$pathWithQuery#$fragment"

        return URI(pathWithQueryAndFragment)
    }

    private fun relativeUri(
        query: String?,
        fragment: String?,
    ) = URI(
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
        query,
        /* fragment = */
        fragment,
    )
}

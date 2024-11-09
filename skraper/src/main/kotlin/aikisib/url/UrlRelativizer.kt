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
    fun relativize(source: URI, target: URI, rawFragment: String?): URI?
}

internal object UrlRelativizerImpl : UrlRelativizer {

    override fun relativize(source: URI, target: URI, rawFragment: String?): URI? {
        require(source.query == null) { "Ненулевой query у source $source" }
        require(target.query == null) { "Ненулевой query у target $target" }

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
            return onlyFragment(target.fragment)
        }

        // Сколько раз надо подняться наверх. Один из сегментов пути -- это имя файла.
        val upDirCount = sourcePathSegments.size - index - 1
        if (upDirCount > 0) {
            return null
        }
        val resultChunks = mutableListOf<String>()
        while (index < targetPathSegments.size) {
            resultChunks.add(targetPathSegments[index])
            ++index
        }

        return relativeUri(resultChunks, rawFragment)
    }

    private fun relativeUri(
        resultChunks: MutableList<String>,
        fragment: String?,
    ): URI {
        val path = resultChunks.joinToString(separator = "/")

        val pathWithFragment = if (fragment.isNullOrBlank()) {
            path
        } else {
            "$path#$fragment"
        }

        return URI(pathWithFragment)
    }

    private fun onlyFragment(
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
        null,
        /* fragment = */
        fragment,
    )
}

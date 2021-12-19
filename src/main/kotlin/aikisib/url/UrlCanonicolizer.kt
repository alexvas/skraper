package aikisib.url

import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPath
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import org.apache.commons.validator.routines.UrlValidator
import java.net.URI

/**
 * Сервис для обработки ссылок
 */
interface UrlCanonicolizer {

    /**
     * Привести ссылку на объект к канонической форме.
     *
     * @param parent - где расположена страничка, откуда взяли ссылку.
     * @param originalUrl - сама ссылка на объект. Может быть абсолютной или относительной.
     * @return ссылка в канонической форме.
     */
    fun canonicalize(parent: URI, originalUrl: String): URI
}

internal object UrlCanonicolizerImpl : UrlCanonicolizer {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))

    /**
     * Отдаёт ссылку на объект в канонической форме.
     *
     * @param parent - где расположена страничка, откуда взяли ссылку.
     * @param originalUrl - сама ссылка на объект
     * @return ссылка в канонической форме.
     */
    override fun canonicalize(parent: URI, originalUrl: String): URI {
        val input = normalize(parent, originalUrl)
        return URLBuilder().takeFrom(input)
            .also { urlBuilder ->
                urlBuilder.encodedPathSegments = input.encodedPathSegments()
            }
            .build()
            .toURI()
            .validated()
    }

    private fun normalize(parent: URI, originalUrl: String): URI {
        val withCanonicalPath = URI.create(originalUrl)
            .normalize()
            .withTrailingSlashFixed()
        if (withCanonicalPath.isAbsolute)
            return withCanonicalPath
        require(parent.isAbsolute) { "Родительский путь должен быть абсолютным, а не относительным: $parent" }
        return parent
            .withEmptyPathFixed()
            .resolve(withCanonicalPath)
            .withTrailingSlashFixed()
    }

    /**
     * Бросаем исключение при невалидном URI
     */
    private fun URI.validated() =
        also {
            require(urlValidator.isValid(this.toString())) {
                "Результирующий URL $this невалидный."
            }
        }

    /**
     * Если путь пустой, устанавливаем его в единственный символ '/'.
     */
    private fun URI.withEmptyPathFixed() =
        if (path.isNullOrEmpty())
            withPath("/")
        else
            this

    /**
     * Удаляем завершающий слэш в пути, если такой имеется.
     */
    private fun URI.withTrailingSlashFixed() =
        if (path.endsWith("/"))
            withPath(path.removeSuffix("/"))
        else
            this
}

/**
 * Подменяем путь.
 */
internal fun URI.withPath(targetPath: String) =
    URI(
        /* scheme = */
        scheme,
        /* userInfo = */
        userInfo,
        /* host = */
        host,
        /* port = */
        port,
        /* path = */
        targetPath,
        /* query = */
        query,
        /* fragment = */
        fragment,
    )

internal fun URI.encodedPathSegments() =
    path.encodedPathSegments()

internal fun String.encodedPathSegments() =
    splitToSequence('/')
        .map { it.encodeURLPath() }
        .toList()

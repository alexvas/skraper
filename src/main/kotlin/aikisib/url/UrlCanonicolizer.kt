package aikisib.url

import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPath
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import org.apache.commons.validator.routines.UrlValidator
import java.net.URI
import java.net.URLDecoder
import kotlin.text.Charsets.UTF_8

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
        if (rawPath.isNullOrEmpty())
            withPath("/")
        else
            this

    /**
     * Удаляем завершающий слэш в пути, если такой имеется.
     */
    private fun URI.withTrailingSlashFixed(): URI {
        if (rawPath?.endsWith("/") != true || rawPath == "/")
            return this

        return URLBuilder().takeFrom(this)
            .also { urlBuilder ->
                val segments = urlBuilder.encodedPathSegments
                val segmentsWoLast = segments.toMutableList()
                segmentsWoLast.removeLast()
                urlBuilder.encodedPathSegments = segmentsWoLast
            }
            .build()
            .toURI()
    }
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

private fun URI.encodedPathSegments() =
    rawPath.splitToSequence('/')
        .map { it.reEncode() }
        .toList()

private fun String.reEncode() = if (isEmpty())
    this
else
    URLDecoder.decode(this, UTF_8)
        .encodeURLPath()
        .replace("/", "%2F")

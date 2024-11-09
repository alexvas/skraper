package aikisib.url

import io.ktor.http.ContentType
import java.net.URI

/**
 * Сервис для трансляции URL к форме, удобной для локального сохранения.
 */
interface UrlTouchdownTransformer {

    /**
     * Добавляем расширение .css стилям.
     * Добавляем query-параметры в путь.
     *
     * @param contentType тип содержимого
     * @param input входная ссылка
     * @return результирующая ссылка на локальную страничку или ресурс
     */
    fun transform(contentType: ContentType, input: URI): LocalResource
}

@Suppress("TooManyFunctions")
internal object UrlTouchdownTransformerImpl : UrlTouchdownTransformer {

    override fun transform(contentType: ContentType, input: URI): LocalResource {
        return when (contentType) {
            ContentType.Text.Html -> LocalResource.fromHtmlPage(input)
            else -> LocalResource.fromEtc(input, contentType)
        }
    }
}

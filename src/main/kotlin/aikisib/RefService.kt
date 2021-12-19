package aikisib

import aikisib.model.ResourceDescription
import java.net.URI


/**
 * Сервис для обработки ссылок
 */
interface RefService {

    /**
     * Определяет тип ресурса, на который указывает ссылка.
     *
     * @param parent - где расположена страничка, откуда взяли ссылку.
     * @param originalUrl - сама ссылка на объект
     * @param canonicalUrl ссылка-указатель
     * @return тип ресурса.
     */
    fun guessResourceDescription(parent: URI, originalUrl: String, canonicalUrl: URI): ResourceDescription
}

object RefServiceImpl : RefService {
    override fun guessResourceDescription(parent: URI, originalUrl: String, canonicalUrl: URI): ResourceDescription {
        TODO("Not yet implemented")
    }
}

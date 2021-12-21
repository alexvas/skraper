package aikisib.mirror

import java.net.URI

/**
 * Фильтр ссылок. Делит ссылки на внутренние и внешние.
 */
interface FromLinkFilter {

    /**
     * Является ли входной URI внутренним для указанного корневого.
     */
    fun filter(input: URI): Boolean
}

internal class FromLinkFilterImpl(
    private val rootUri: URI,
) : FromLinkFilter {
    override fun filter(input: URI): Boolean {
        val maybeRelative = rootUri.relativize(input)
        return maybeRelative != input
    }
}

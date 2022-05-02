package aikisib

import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.Sources
import java.io.File
import java.net.URL

@Sources("classpath:main.properties")
interface MainConfig : Config {

    /**
     * Корневой URL публичного сайта, который надо отзеркалить.
     */
    fun rootMain(): URL

    /**
     * Альтернативные псевдонимы сайта, куда могут ссылаться ресурсы.
     */
    fun rootAliases(): List<URL>

    /**
     * Целевой URL, который применяется для
     * ```
     * <link rel="canonical" href="bla-bla-bla">,
     * ```
     * где href должен быть абсолютной ссылкой.
     */
    fun canonicalHref(): URL

    /**
     * Ненужные для зеркалирования префиксы.
     */
    fun ignoredPrefixes(): List<String>

    /**
     * Ненужные для зеркалирования суффиксы.
     */
    fun ignoredSuffixes(): List<String>

    /**
     * Ненужные для зеркалирования типы содержимого.
     */
    fun ignoredContentTypes(): List<String>

    /**
     * Путь к утилите cwebp.
     */
    fun cwebpExecutable(): String

    /**
     * Путь ко временной директории для скачиваемых файлов.
     */
    fun tempDir(): File

    /**
     * Путь к целевой директории, куда сохранять зеркалируемые файлы.
     * Директория очищается вначале работы приложения.
     */
    fun mirrorDir(): File
}

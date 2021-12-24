package aikisib

import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.Sources
import java.net.URL

@Sources("classpath:main.properties")
interface MainConfig : Config {

    /**
     * Корневой URL публичного сайта.
     */
    fun publicUrl(): URL

    /**
     * Ненужные для зеркалирования префиксы
     */
    fun forbiddenPrefixes(): List<String>
    /**
     * Ненужные для зеркалирования суффиксы
     */
    fun forbiddenSuffixes(): List<String>
}

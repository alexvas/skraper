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
}

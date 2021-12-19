package aikisib

import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.Sources
import java.net.URL

@Sources("classpath:sliderRepo.properties")
interface SliderRepo : Config {

    /**
     * Корневой URL сайта, где доступна админка
     */
    fun adminUrl(): URL

    /**
     * Идентификаторы слайдеров, которые хочется экспортировать.
     * Если идентификаторов несколько, перечисляем их через запятую.
     */
    @DefaultValue("5")
    fun sliderIds(): List<Int>
}

package aikisib.contact7

import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.Sources

@Sources("classpath:contact7.properties")
interface Contact7Config : Config {

    /**
     * Хост, на котором будет работать сервер.
     */
    fun serverHost(): String

    /**
     * Порт, на котором будет работать сервер.
     */
    fun serverPort(): Int

    /**
     * Секрет Гугловой рекапчи
     */
    fun reCaptchaSecret(): String

    /**
     * Идентификатор бота Телеги, cмотри @BotFather.
     */
    fun telegramBotId(): String

    /**
     * Идентификатор канала Телеги,куда надо пересылать сообщения из ContactForm7.
     */
    fun telegramChatId(): Long
}

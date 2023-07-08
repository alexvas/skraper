package aikisib.contact7

import kotlinx.serialization.Serializable

@Serializable
data class Contact7Config(
    val server: ServerConfig,
    val yandex: YandexConfig,
    val vk: VkConfig,
    val telegram: TelegramConfig,
)

/**
 * Где сервер будет отдавать страничку с формой и принимать POST-запросы.
 */
@Serializable
data class ServerConfig(
    val host: String,

    val port: Int,
)

/**
 * Интеграция с Яндексом
 */
@Serializable
data class YandexConfig(
    /**
     * Секрет капчи
     */
    val secret: String,

    /**
     * Счётчик посещения страницы
     */
    val counter: Map<String, Long>,
)

/**
 * Интеграция с Яндексом
 */
@Serializable
data class VkConfig(
    /**
     * Счётчик посещения страницы
     */
    val counter: Map<String, Long>,
)

/**
 * Интеграция с Телеграмом.
 */
@Serializable
data class TelegramConfig(
    /**
     * Идентификатор бота, cмотри @BotFather.
     */
    val botId: String,

    /**
     * Идентификатор канала Телеги, куда надо пересылать сообщения из ContactForm7.
     */
    val chatId: Long,
)

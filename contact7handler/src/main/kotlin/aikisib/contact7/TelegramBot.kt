package aikisib.contact7

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import mu.KLogging

interface TelegramBot {

    /**
     * Отсылаем сообщение в канал Телеги
     *
     * @param content - набор ключ-значение содержимого к отправке
     * @return true, если отослали успешно
     */
    suspend fun send(content: Map<String, String>): Boolean
}

internal class TelegramBotImpl(
    private val config: TelegramConfig,
    private val client: HttpClient,
) : TelegramBot {

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun send(content: Map<String, String>): Boolean {
        val endpoint = "https://api.telegram.org/bot${config.botId}/sendMessage"

        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(createMessageToSend(content))
        }
        if (response.status != HttpStatusCode.OK) {
            logger.warn { "Статус ${response.status} для обращения к телеграм-боту." }
        }

        val parsed: TelegramResponse = try {
            response.body()
        } catch (e: Exception) {
            val textResponse = try {
                response.bodyAsText()
            } catch (ignored: Exception) {
                null
            }
            logger.warn(e) { "Не удалось расшифровать ответ телеграм-бота: '$textResponse'" }
            return false
        }
        if (parsed.ok) {
            return true
        }

        logger.warn { "Не удалось отправить запрос в канал бота: ${parsed.description}" }
        return false
    }

    private fun createMessageToSend(content: Map<String, String>): MessageToSend {
        val output = buildList {
            content.filterNot {
                it.key.startsWith("_wpcf7") || it.key == "smart-token"
            }.forEach { (key, value) ->
                add("$key => $value")
            }
        }
        val textContent = output.joinToString(separator = "\n")
        return MessageToSend(
            chat_id = config.chatId,
            text = textContent,
//            parse_mode = "MarkdownV2",
            disable_web_page_preview = true,
        )
    }

    companion object : KLogging()
}

@Suppress("ConstructorParameterNaming")
@Serializable
private data class MessageToSend(
    val chat_id: Long,
    val text: String,
    val parse_mode: String? = null,
    val disable_web_page_preview: Boolean,
)

@Serializable
private data class TelegramResponse(
    val ok: Boolean,
    val description: String? = null,
)

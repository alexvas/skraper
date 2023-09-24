package aikisib.slider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.http.takeFrom
import mu.KLogging
import java.io.File
import java.net.URL

interface SliderRevolutionScraper {

    /**
     * Входит в админку вордпресса с данными именем пользователя и паролем
     *
     * @param wordpressLoginPath -- адрес странички входа
     * @param username -- имя пользователя с правами администратора
     * @param password -- его пароль
     */
    suspend fun loginIntoWordpress(wordpressLoginPath: String, username: String, password: String): Boolean

    /**
     * Выбираем подменю плагина "Slider Revolution"
     * @return nonce -- временную уникальную комбинацию символов
     */
    suspend fun navigateToSliderRevolutionPage(): String

    /**
     * Загружаем указанный модуль (например, слайдер) плагина Slider Revolution
     * как zip-архив в HTML-исходниками.
     * @param id -- идентификатор модуля
     * @param nonce -- временная уникальная комбинация символов
     * @return байтовый массив с zip-архивом модуля
     */
    suspend fun downloadModuleZip(id: Int, nonce: String): ByteArray
}

class SliderRevolutionScraperImpl(
    /**
     * Корневой URL сайта, где доступна админка
     */
    private val adminUrl: URL,
) : SliderRevolutionScraper {

    private val client = HttpClient(Apache) {
        engine {
            socketTimeout = 10_000
            connectTimeout = 10_000
            connectionRequestTimeout = 20_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    logger().trace { message }
                }
            }
            level = LogLevel.INFO
        }
        install(HttpCookies)
    }

    override suspend fun loginIntoWordpress(wordpressLoginPath: String, username: String, password: String): Boolean {
        logger.info { "logging into Wordpress: $wordpressLoginPath as $username" }
        try {
            doLoginIntoWordpress(wordpressLoginPath, username, password)
        } catch (e: RedirectResponseException) {
            if (e.response.status == HttpStatusCode.Found) {
                return true
            }
        }
        return false
    }

    private suspend fun doLoginIntoWordpress(wordpressLoginPath: String, username: String, password: String) {
        client.post {
            forPath(wordpressLoginPath)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("log", username)
                        append("pwd", password)
                    },
                ),
            )
        }
    }

    override suspend fun navigateToSliderRevolutionPage(): String {
        logger.info { "Navigate to Slider Revolution page." }
        val content = client.get {
            forPath(REV_SLIDER_PATH)
        }.bodyAsText()
        File("/tmp/slider_revolution_page.html").also {
            it.createNewFile()
            it.writeText(content)
        }
        return parseNonce(content) ?: error("Nonce not found at $REV_SLIDER_PATH!")
    }

    override suspend fun downloadModuleZip(id: Int, nonce: String): ByteArray =
        client.post {
            forPath(downloadPath(id, nonce))
        }.body()

    private fun parseNonce(input: String) =
        NONCE_REGEX.find(input)?.groupValues?.get(1)

    private fun HttpRequestBuilder.forPath(path: String) =
        url {
            takeFrom(adminUrl)
            val chunks = path.split('?')
            when (chunks.size) {
                1 -> path(path)
                2 -> pathWithArgs(path, chunks)
                else -> throw IllegalArgumentException("непонятный путь: '$path'")
            }
        }

    private fun URLBuilder.pathWithArgs(path: String, argVals: List<String>) {
        val reallyPath = argVals[0]
        path(reallyPath)
        argVals[1].splitToSequence('&')
            .forEach { mayBePair -> appendArgs(mayBePair, path) }
    }

    private fun URLBuilder.appendArgs(mayBePair: String, path: String) {
        val argVal = mayBePair.split('=')
        when (argVal.size) {
            1 -> parameters.append(mayBePair, "")
            2 -> parameters.append(argVal[0], argVal[1])
            else -> throw IllegalArgumentException("непонятные аргументы: '$path'")
        }
    }

    private fun downloadPath(id: Int, nonce: String) =
        "/wp-admin/admin-ajax.php?action=revslider_ajax_action&client_action=export_slider_html&nonce=$nonce&id=$id"

    companion object : KLogging() {
        private const val REV_SLIDER_PATH = "/wp-admin/admin.php?page=revslider"
        internal val NONCE_REGEX = Regex("""RVS\.ENV\.nonce\s*=\s*'([^']+)';""")
    }
}

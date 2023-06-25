package aikisib.contact7

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import kotlinx.serialization.json.Json
import mu.KLoggable
import mu.KotlinLogging.logger
import org.aeonbits.owner.Config
import org.aeonbits.owner.ConfigFactory
import kotlin.reflect.KClass
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val logger = logger("contact7")

private const val X_REAL_IP = "X-Real-IP"

suspend fun main() {
    Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val config = createConfig(Contact7Config::class)

    val httpClient = createHttpClient()
    val contact7FormValidator: Contact7FormValidator = Contact7FormValidatorImpl
    val captchaChecker: CaptchaChecker = CaptchaCheckerImpl(config.yaCaptchaSecret(), httpClient)
    val telegramBot: TelegramBot = TelegramBotImpl(config.telegramBotId(), config.telegramChatId(), httpClient)

    val contact7handler: Contact7Handler = Contact7HandlerImpl(contact7FormValidator, captchaChecker, telegramBot)

    val tildaHandler: TildaHandler = TildaHandlerImpl(captchaChecker, telegramBot)

    fun Application.module() {
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, status ->
                call.respondText(text = "404: Page Not Found (Нетути)", status = status)
            }
            exception<Throwable> { call, cause ->
                logger.error(cause) { "Внутренняя ошибка сервера" }
                call.respondText(text = "500: внутренняя ошибка сервера", status = HttpStatusCode.InternalServerError)
            }
        }
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                },
            )
        }
        routing {
            get("/{...}") {
                call.respondText("")
            }

            post("/wp-json/contact-form-7/v1/contact-forms/{formNum}/feedback") {
                val referer = call.request.header(HttpHeaders.Referrer)
                val ipAddress = call.request.header(X_REAL_IP)

                val formParameters = call.receiveParameters().toMap().mapValues { it.value.firstOrNull() }
                val feedback = contact7handler.handleRequest(referer, ipAddress, formParameters) ?: return@post
                call.respond(feedback)
            }

            post("/do_apply") {
                val ipAddress = call.request.header(X_REAL_IP)
                val formParameters = call.receiveParameters().toMap().mapValues { it.value.firstOrNull() }
                val status = if (tildaHandler.handleRequest(ipAddress, formParameters)) {
                    HttpStatusCode.OK
                } else {
                    HttpStatusCode.BadRequest
                }
                call.respond(status)
            }
        }
    }

    embeddedServer(
        factory = CIO,
        host = config.serverHost(),
        port = config.serverPort(),
        module = { module() },
    ).start(wait = true)
}

private fun createHttpClient() =
    HttpClient(io.ktor.client.engine.cio.CIO) {
        expectSuccess = false
        install(ClientContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    CaptchaCheckerImpl.logger().trace { message }
                }
            }
            level = LogLevel.INFO
        }
    }

private fun <T : Config> createConfig(kClass: KClass<T>): T =
    ConfigFactory.create(kClass.java)!!

/**
 * Last resort exception handler
 */
internal class DefaultUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, KLoggable {
    override val logger = logger()

    override fun uncaughtException(t: Thread, e: Throwable) =
        logger.error(e) { "Unhandled exception" }
}

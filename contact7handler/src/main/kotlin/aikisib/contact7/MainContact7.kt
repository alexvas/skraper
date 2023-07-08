package aikisib.contact7

import com.typesafe.config.ConfigFactory
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
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.util.toMap
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.json.Json
import mu.KLoggable
import mu.KotlinLogging.logger
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val logger = logger("contact7")

private const val X_REAL_IP = "X-Real-IP"

suspend fun main() {
    Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val config = ConfigFactory.load("app.hocon.conf")
    val appConfig: Contact7Config = Hocon.decodeFromConfig(config)

    val httpClient = createHttpClient()
    val contact7FormValidator: Contact7FormValidator = Contact7FormValidatorImpl
    val captchaChecker: CaptchaChecker = CaptchaCheckerImpl(appConfig.yandex.secret, httpClient)
    val telegramBot: TelegramBot = TelegramBotImpl(appConfig.telegram, httpClient)

    val contact7handler: Contact7Handler = Contact7HandlerImpl(contact7FormValidator, captchaChecker, telegramBot)

    val tildaHandler: TildaHandler = TildaHandlerImpl(captchaChecker, telegramBot)

    @Suppress("LongMethod")
    fun Application.module() {
        install(Thymeleaf) {
            setTemplateResolver(
                ClassLoaderTemplateResolver().apply {
                    prefix = "apply/"
                    suffix = ".html"
                    characterEncoding = "utf-8"
                },
            )
        }
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
                val ipAddress = call.request.header(X_REAL_IP)

                val formParameters = call.receiveParameters()
                    .toMap()
                    .mapValues { it.value.firstOrNull() }
                    .toMutableMap()
                formParameters["landing"] = call.request.header(HttpHeaders.Referrer)
                formParameters["ip"] = ipAddress
                val feedback = contact7handler.handleRequest(ipAddress, formParameters) ?: return@post
                call.respond(feedback)
            }

            staticResources("/static", "apply/static") {
                enableAutoHeadResponse()
            }
            get("/apply") {
                val landing = call.request.queryParameters["landing"] ?: "unknown"
                val yaCounter = appConfig.yandex.counter[landing]
                if (yaCounter == null) {
                    call.respondText { "Landing $landing has no Yandex counter configured." }
                    return@get
                }
                val vkCounter = appConfig.vk.counter[landing]
                if (vkCounter == null) {
                    call.respondText { "Landing $landing has no Vkontakte Pixel counter configured." }
                    return@get
                }
                call.respond(
                    ThymeleafContent(
                        "index",
                        mapOf(
                            "landing" to landing,
                            "ya_counter_num" to yaCounter,
                            "vk_counter_num" to vkCounter,
                        ),
                    ),
                )
            }
            post("/do_apply") {
                val ipAddress = call.request.header(X_REAL_IP)
                val formParameters = call.receiveParameters()
                    .toMap()
                    .mapValues { it.value.firstOrNull() }
                    .toMutableMap()
                val host = call.request.header(HttpHeaders.XForwardedHost)
                if (host == null) {
                    logger.debug { "отсутствует заголовок X-Forwarded-Host" }
                    return@post
                }
                formParameters["landing"] = host
                formParameters["ip"] = ipAddress
                val status = if (tildaHandler.handleRequest(ipAddress, formParameters)) {
                    HttpStatusCode.OK
                } else {
                    HttpStatusCode.BadRequest
                }
                call.respond(status)
            }
        }
    }

    val serverConfig = appConfig.server
    embeddedServer(
        factory = CIO,
        host = serverConfig.host,
        port = serverConfig.port,
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

/**
 * Last resort exception handler
 */
internal class DefaultUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, KLoggable {
    override val logger = logger()

    override fun uncaughtException(t: Thread, e: Throwable) =
        logger.error(e) { "Unhandled exception" }
}

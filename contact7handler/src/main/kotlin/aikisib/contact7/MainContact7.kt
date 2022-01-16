package aikisib.contact7

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.ContentNegotiation
import io.ktor.server.plugins.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
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

private val logger = logger("contact7")

suspend fun main() {
    Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val config = createConfig(Contact7Config::class)

    val validator: Contact7FormValidator = Contact7FormValidatorImpl
    val reCaptchaChecker: ReCaptchaChecker = ReCaptchaCheckerImpl(config.reCaptchaSecret())

    val handler: Contact7Handler = Contact7HandlerImpl(validator, reCaptchaChecker)

    embeddedServer(
        factory = CIO,
        host = config.serverHost(),
        port = config.serverPort(),
    ) {
        install(StatusPages) {
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
        install(Routing) {
            routing {
                get("/{...}") {
                    call.respondText("")
                }

                post("/wp-json/contact-form-7/v1/contact-forms/{formNum}/feedback") {
                    val referer = call.request.header("referer")

                    val formParameters = call.receiveParameters().toMap().mapValues { it.value.firstOrNull() }
                    val feedback = handler.handleRequest(referer, formParameters) ?: return@post
                    call.respond(feedback)
                }
            }
        }
    }.start(wait = true)
}

private fun <T : Config> createConfig(kClass: KClass<T>): T =
    ConfigFactory.create(kClass.java)

/**
 * Last resort exception handler
 */
internal class DefaultUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, KLoggable {
    override val logger = logger()

    override fun uncaughtException(t: Thread, e: Throwable) =
        logger.error(e) { "Unhandled exception" }
}

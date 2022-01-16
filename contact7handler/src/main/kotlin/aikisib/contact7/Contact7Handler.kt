package aikisib.contact7

import mu.KLogging
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

interface Contact7Handler {

    suspend fun handleRequest(
        referer: String?,
        formParameters: Map<String, String?>,
    ): Feedback?
}

@Suppress("ReturnCount")
class Contact7HandlerImpl(
    private val formValidator: Contact7FormValidator,
    private val reCaptchaChecker: ReCaptchaChecker,
    private val telegramBot: TelegramBot,
) : Contact7Handler {

    /**
     * Обрабатывает запрос и возвращает ответ клиенту
     */
    override suspend fun handleRequest(
        referer: String?,
        formParameters: Map<String, String?>,
    ): Feedback? {

        val contactFormId: Int = formParameters.intParameter("_wpcf7") ?: return null
        val pageId: Int = formParameters.intParameter("_wpcf7_container_post") ?: return null

        val validationResult = formValidator.validate(formParameters)
        if (validationResult.isNotEmpty()) {
            return Feedback.validationFailed(
                contactFormId = contactFormId,
                pageId = pageId,
                failed = validationResult,
            )
        }

        val reCaptchaResponseToken = formParameters["_wpcf7_recaptcha_response"]!!
        val captchaValidationResult = reCaptchaChecker.validate(reCaptchaResponseToken)
            ?: return Feedback.mailSent(
                contactFormId = contactFormId,
                pageId = pageId,
            )

        if (!captchaValidationResult)
            return Feedback.spam(
                contactFormId = contactFormId,
                pageId = pageId,
            )

        dumpOutput(formParameters)
        val filtered = formParameters.asSequence()
            .map { (key, value) -> if (value == null) null else key to value }
            .filterNotNull()
            .toMap()

        val sendTelegramResult = telegramBot.send(referer, filtered)

        return if (sendTelegramResult)
            Feedback.mailSent(
                contactFormId = contactFormId,
                pageId = pageId,
            )
        else
            Feedback.aborted(
                contactFormId = contactFormId,
                pageId = pageId,
            )
    }

    private fun dumpOutput(formParameters: Map<String, String?>) {
        val output = buildList {
            add("###############################################")
            formParameters.filterNot {
                it.key.startsWith("_wpcf7")
            }.forEach { (key, value) ->
                add("""$key => '$value'""")
            }
            add("###############################################")
        }

        val fileOutput = Paths.get("/tmp/contact7.txt")
        fileOutput.writeText(output.joinToString(separator = "\n"), options = arrayOf(StandardOpenOption.APPEND))
    }

    private fun Map<String, String?>.intParameter(name: String): Int? {
        val content = get(name).normalize()
        return try {
            content.toInt()
        } catch (e: NumberFormatException) {
            logger.info { "Входящий запрос без требуемого параметра или с некорректным параметром '$name' = $content" }
            null
        }
    }

    private fun String?.normalize() =
        when {
            this == null -> ""
            this.length > IDENTITY_MAX_LENGTH -> "${this.substring(0, IDENTITY_MAX_LENGTH)}..."
            else -> this
        }

    companion object : KLogging() {
        const val IDENTITY_MAX_LENGTH = 5
    }
}

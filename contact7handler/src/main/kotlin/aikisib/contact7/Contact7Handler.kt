package aikisib.contact7

import mu.KLogging
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

interface Contact7Handler {

    suspend fun handleRequest(
        ipAddress: String?,
        formParameters: Map<String, String?>,
    ): Feedback?
}

@Suppress("ReturnCount")
class Contact7HandlerImpl(
    private val formValidator: Contact7FormValidator,
    private val captchaChecker: CaptchaChecker,
    private val telegramBot: TelegramBot,
) : Contact7Handler {

    /**
     * Обрабатывает запрос и возвращает ответ клиенту
     */
    override suspend fun handleRequest(
        ipAddress: String?,
        formParameters: Map<String, String?>,
    ): Feedback? {
        val contactFormId: Int = formParameters.intParameter("_wpcf7") ?: let {
            logger.info { "нет идентификатора формы" }
            return null
        }
        val pageId: Int = formParameters.intParameter("_wpcf7_container_post") ?: let {
            logger.info { "нет номера страницы" }
            return null
        }

        val validationResult = formValidator.validate(formParameters)
        if (validationResult.isNotEmpty()) {
            return Feedback.validationFailed(
                contactFormId = contactFormId,
                pageId = pageId,
                failed = validationResult,
            )
        }

        val captchaResponseToken = formParameters["smart-token"] ?: let {
            logger.info { "нет токена Яндекса" }
            return null
        }
        val ip = ipAddress ?: let {
            logger.info { "нет ip-адреса" }
            return null
        }
        val dataHash = formParameters.entries
            .map { "${it.key}=${it.value}" }
            .reduce { acc, cur -> acc + cur }
            .toMD5()

        val captchaValidationResult = captchaChecker.validate(captchaResponseToken, ip, dataHash)
            ?: return Feedback.mailSent(
                contactFormId = contactFormId,
                pageId = pageId,
            )

        if (!captchaValidationResult) {
            return Feedback.spam(
                contactFormId = contactFormId,
                pageId = pageId,
            )
        }

        dumpOutput(formParameters)
        val filtered = formParameters.asSequence()
            .map { (key, value) -> if (value.isNullOrBlank()) null else key to value }
            .filterNotNull()
            .toMap()

        val sendTelegramResult = telegramBot.send(filtered)

        return if (sendTelegramResult) {
            Feedback.mailSent(
                contactFormId = contactFormId,
                pageId = pageId,
            )
        } else {
            Feedback.aborted(
                contactFormId = contactFormId,
                pageId = pageId,
            )
        }
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
        if (!fileOutput.exists()) {
            fileOutput.createFile()
        }
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

internal fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.toHex()
}

private fun ByteArray.toHex() =
    joinToString("") { "%02x".format(it) }

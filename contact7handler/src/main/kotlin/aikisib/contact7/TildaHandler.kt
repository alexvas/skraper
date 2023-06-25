package aikisib.contact7

import mu.KLogging
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

interface TildaHandler {

    suspend fun handleRequest(
        ipAddress: String?,
        formParameters: Map<String, String?>,
    ): Boolean
}

@Suppress("ReturnCount")
class TildaHandlerImpl(
    private val captchaChecker: CaptchaChecker,
    private val telegramBot: TelegramBot,
) : TildaHandler, KLogging() {

    /**
     * Обрабатывает запрос и возвращает ответ клиенту
     */
    override suspend fun handleRequest(
        ipAddress: String?,
        formParameters: Map<String, String?>,
    ): Boolean {
        /*
                val validationResult = formValidator.validate(formParameters)
                if (validationResult.isNotEmpty()) {
                    return
                }
        */

        dumpOutput(formParameters)
        val captchaResponseToken = formParameters["smart-token"] ?: let {
            logger.debug { "нет токена Яндекса" }
            return false
        }
        val ip = ipAddress ?: let {
            logger.debug { "нет ip-адреса" }
            return false
        }
        val dataHash = formParameters.entries
            .map { "${it.key}=${it.value}" }
            .reduce { acc, cur -> acc + cur }
            .toMD5()

        val captchaValidationResult = captchaChecker.validate(captchaResponseToken, ip, dataHash)
        if (captchaValidationResult != true) {
            return false
        }

        val filtered = formParameters.asSequence()
            .map { (key, value) -> if (value == null) null else key to value }
            .filterNotNull()
            .toMap()

        telegramBot.send(null, filtered)
        return true
    }

    private fun dumpOutput(formParameters: Map<String, String?>) {
        val output = buildList {
            add("###############################################")
            formParameters.forEach { (key, value) ->
                add("""$key => '$value'""")
            }
            add("###############################################")
        }

        val fileOutput = Paths.get("/tmp/tilda.txt")
        if (!fileOutput.exists()) {
            fileOutput.createFile()
        }
        fileOutput.writeText(output.joinToString(separator = "\n"), options = arrayOf(StandardOpenOption.APPEND))
    }
}
